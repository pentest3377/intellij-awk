package intellij_awk.psi;

import com.intellij.lang.ASTNode;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.stubs.IStubElementType;
import intellij_awk.AwkReferenceVariable;
import intellij_awk.AwkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public abstract class AwkUserVarNameMixin
    extends AwkNamedStubBasedPsiElementBase<AwkUserVarNameStub> implements AwkUserVarName {

  /**
   *
   * <li>Var = ...
   * <li>Var["key"] = ...
   */
  private static final PsiElementPattern.@NotNull Capture<PsiElement> LVALUE_ASSIGN =
      psiElement()
          .withParent(
              (psiElement(AwkLvalue.class).beforeLeaf(psiElement(AwkTypes.ASSIGN)))
                  .withParent(
                      psiElement(AwkNonUnaryExpr.class)
                          .withParent(psiElement(AwkSimpleStatement.class))));

  /**
   *
   * <li>Var["key"]
   */
  private static final PsiElementPattern.@NotNull Capture<PsiElement> LVALUE_ASSIGN1 =
      psiElement()
          .withParent(
              psiElement(AwkLvalue.class)
                  .withParent(
                      psiElement(AwkNonUnaryExpr.class)
                          .withParent(psiElement(AwkSimpleStatement.class))));

//  private static final PsiElementPattern.@NotNull Capture<PsiElement> INSIDE_BEGIN =
//      psiElement()
//          .inside(Awk);

  public AwkUserVarNameMixin(@NotNull ASTNode node) {
    super(node);
  }

  public AwkUserVarNameMixin(@NotNull AwkUserVarNameStub stub, @NotNull IStubElementType nodeType) {
    super(stub, nodeType);
  }

  public PsiElement setName(String newName) {
    return replaceNameNode(AwkElementFactory.createUserVarName(getProject(), newName));
  }

  @Override
  public AwkReferenceVariable getReference() {
    return new AwkReferenceVariable(this, getNameTextRange());
  }

  @Override
  public @Nullable Icon getIcon(int flags) {
    return null;
  }

  /**
   *
   * <li>Var = ...
   * <li>Var["key"] = ...
   * <li>Var["key"]
   * <li>split("",Var)
   */
  public boolean looksLikeDeclaration() {
    AwkUserVarNameStub stub = getStub();
    if (stub != null) {
      return stub.looksLikeDeclaration();
    }

    if (LVALUE_ASSIGN.accepts(this)) {
      return true;
    }

    // TODO can this be done easier???
    if (LVALUE_ASSIGN1.accepts(this)
        && getParent().getParent().getFirstChild().getNextSibling() == null) {
      return true;
    }

    PsiElement p = AwkUtil.findParent(this, AwkGawkFuncCallList.class);
    if (p != null) {
      p = p.getParent();
      ASTNode splitFunc;
      if (p != null
          && (splitFunc = p.getFirstChild().getNode())
              .getElementType()
              .equals(AwkTypes.BUILTIN_FUNC_NAME)
          && splitFunc.getText().equals("split")
          && p.getText().replace(" ", "").equals("split(\"\"," + getName() + ")")) {
        // `split("",Var)` case
        return true;
      }
    }

    return false;
  }

//  public boolean isInsideInitializingContext() {
//
//  }
}
