package intellij_awk;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.util.IncorrectOperationException;
import intellij_awk.psi.*;
import intellij_awk.psi.impl.AwkItemImpl;
import intellij_awk.psi.impl.AwkUserVarNameImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class AwkReferenceVariable extends PsiReferenceBase<AwkNamedElement>
    implements PsiPolyVariantReference {

  public AwkReferenceVariable(@NotNull AwkNamedElement element, TextRange rangeInElement) {
    super(element, rangeInElement);
  }

  @Override
  public Resolved.AwkResolvedResult @NotNull [] multiResolve(boolean incompleteCode) {
    List<Resolved.AwkResolvedResult> res = new ArrayList<>();

    Resolved ref = resolveFunctionArgument("RESOLVE-ARG", myElement);
    if (ref == null) {
      ref = resolveGlobalVariableDeclarationsInCurrentFileInitCtx("RESOLVE-CUR-INIT-DECL", myElement);
    }
    if (ref == null) {
      ref =
          resolveGlobalVariableInProjectFiles("GlobalVariableInProjectFiles-decl", true, myElement);
    }
    if (ref == null) {
      ref =
          resolveGlobalVariableInProjectFiles("GlobalVariableInProjectFiles-use", false, myElement);
    }
    if (ref != null) {
      Resolved.AwkResolvedResult resolvedResult = ref.toResolvedResult();
      if (resolvedResult != null) {
        res.add(resolvedResult);
      }
    }
    return res.toArray(new AwkReferenceVariable.Resolved.AwkResolvedResult[0]);
  }

  private @Nullable Resolved resolveFunctionArgument(String type, AwkNamedElement userVarName) {
    PsiElement parent = userVarName;
    while (true) {
      parent = parent.getParent();
      if (parent == null || parent instanceof AwkFile) {
        break;
      }
      if (parent instanceof AwkItemImpl) {
        AwkItemImpl awkItem = (AwkItemImpl) parent;
        if (awkItem.getFunctionName() != null) {
          AwkParamList paramList = awkItem.getParamList();
          if (paramList != null) {
            List<AwkUserVarName> userVarNameList = paramList.getUserVarNameList();
            for (AwkUserVarName awkUserVarName : userVarNameList) {
              PsiElement varName = awkUserVarName.getVarName();
              if (varName.textMatches(userVarName.getName())) {
                if (awkUserVarName == userVarName) {
                  return new Resolved(type, null); // no need to display a reference to itself
                }
                return new Resolved(type, awkUserVarName);
              }
            }
          }
        }
      }
    }
    return null;
  }

  private Resolved resolveGlobalVariableDeclarationsInCurrentFileInitCtx(String type, AwkNamedElement userVarName) {
    AwkFile awkFile = (AwkFile) userVarName.getContainingFile();

    Resolved resolved = null;
    PsiElement varDeclaration =
        AwkUtil.findFirstMatchedDeep(
            awkFile,
            psiElement ->
                psiElement instanceof AwkUserVarName
                    && ((AwkUserVarName) psiElement).getVarName().textMatches(userVarName.getName())
                    && ((AwkUserVarNameMixin) psiElement).looksLikeDeclaration());
    if (varDeclaration != null) {
      if (varDeclaration == userVarName) {
        resolved = new Resolved(type, null); // no need to display a reference to itself
      } else {
        resolved = new Resolved(type, varDeclaration);
      }
    }

    return resolved;
  }

  private @Nullable Resolved resolveGlobalVariableInProjectFiles(
      String type, boolean searchDeclarations, AwkNamedElement userVarName) {
    Collection<AwkUserVarNameImpl> userVarDeclarations =
        AwkUtil.findUserVars(searchDeclarations, userVarName.getProject(), userVarName.getName());
    Resolved resolved = null;
    if (!userVarDeclarations.isEmpty()) {
      AwkUserVarNameImpl found = userVarDeclarations.iterator().next();
      if (found == userVarName) {
        resolved = new Resolved(type, null); // no need to display a reference to itself
      } else {
        resolved = new Resolved(type, found);
      }
    }
    return resolved;
  }

  /**
   *
   * <li>resolved == null : proceed resolution
   * <li>resolved != null : use as resolved result
   * <li>resolved.value == null : meaning resolved to itself
   */
  public static class Resolved {
    @Nullable private final PsiElement value;
    private final String type;

    Resolved(String type, @Nullable PsiElement value) {
      this.type = type;
      this.value = value;
    }

    private AwkResolvedResult toResolvedResult() {
      return value == null ? null : new AwkResolvedResult(type, value);
    }

    public static class AwkResolvedResult extends PsiElementResolveResult {
      public final String type;

      public AwkResolvedResult(String type, @NotNull PsiElement element) {
        super(element);
        this.type = type;
      }
    }
  }

  /** Resolves references to a single result, or fails. */
  @Override
  public @Nullable PsiElement resolve() {
    ResolveResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0].getElement() : null;
  }

  /** Resolves references to a single result, or fails. */
  public @Nullable AwkReferenceVariable.Resolved.AwkResolvedResult resolveResult() {
    Resolved.AwkResolvedResult[] resolveResults = multiResolve(false);
    return resolveResults.length == 1 ? resolveResults[0] : null;
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName)
      throws IncorrectOperationException {
    return myElement.setName(newElementName);
  }
}
