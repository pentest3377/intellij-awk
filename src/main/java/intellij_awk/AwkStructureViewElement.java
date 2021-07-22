package intellij_awk;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.NavigatablePsiElement;
import com.intellij.psi.PsiElement;
import intellij_awk.psi.*;
import intellij_awk.psi.impl.AwkBeginOrEndImpl;
import intellij_awk.psi.impl.AwkFunctionNameImpl;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class AwkStructureViewElement implements StructureViewTreeElement, SortableTreeElement {

  private final NavigatablePsiElement myElement;

  public AwkStructureViewElement(NavigatablePsiElement element) {
    this.myElement = element;
  }

  @Override
  public Object getValue() {
    return myElement;
  }

  @Override
  public void navigate(boolean requestFocus) {
    myElement.navigate(requestFocus);
  }

  @Override
  public boolean canNavigate() {
    return myElement.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myElement.canNavigateToSource();
  }

  @NotNull
  @Override
  public String getAlphaSortKey() {
    if (myElement instanceof AwkBeginOrEndImpl) {
      AwkBeginOrEndImpl beginOrEnd = (AwkBeginOrEndImpl) myElement;
      return "001" + beginOrEnd.getName();
    } else if (myElement instanceof AwkFunctionNameImpl) {
      AwkFunctionNameImpl functionName = (AwkFunctionNameImpl) myElement;
      return "002" + functionName.getName();
    }
    return "???";
  }

  @NotNull
  @Override
  public ItemPresentation getPresentation() {
    ItemPresentation presentation = myElement.getPresentation();
    return presentation != null ? presentation : new PresentationData();
  }

  @NotNull
  @Override
  public TreeElement[] getChildren() {
    if (myElement instanceof AwkFile) {
      List<TreeElement> treeElements = new ArrayList<>();
      AwkFile awkFile = (AwkFile) myElement;

      for (PsiElement child : awkFile.getChildren()) {

        if (child instanceof AwkItem) {
          AwkItem awkItem = (AwkItem) child;

          AwkFunctionName functionName = awkItem.getFunctionName();
          if (functionName != null) {
            treeElements.add(new AwkStructureViewElement((NavigatablePsiElement) functionName));

          } else {
            AwkPattern awkPattern = awkItem.getPattern();
            if (awkPattern != null) {
              AwkBeginOrEnd beginOrEnd = awkPattern.getBeginOrEnd();
              if (beginOrEnd != null) {
                treeElements.add(new AwkStructureViewElement((NavigatablePsiElement) beginOrEnd));
              }
            }
          }
        }
      }
      return treeElements.toArray(new TreeElement[0]);
    }
    return EMPTY_ARRAY;
  }
}
