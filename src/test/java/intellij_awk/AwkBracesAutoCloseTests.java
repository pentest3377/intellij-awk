package intellij_awk;

import com.intellij.testFramework.fixtures.BasePlatformTestCase;

public class AwkBracesAutoCloseTests extends BasePlatformTestCase {

  public void testParen() {
    doTest('(', "BEGIN { a<caret> }", "BEGIN { a(<caret>) }");
  }

  public void testBrace() {
    doTest('{', "BEGIN <caret>", "BEGIN {<caret>}");
  }

  public void testBracket() {
    doTest('[', "BEGIN { a<caret> }", "BEGIN { a[<caret>] }");
  }

  private void doTest(char brace, String code, String expectedCode) {
    myFixture.configureByText("a.awk", code);
    myFixture.type(brace);
    myFixture.checkResult(expectedCode);
  }
}
