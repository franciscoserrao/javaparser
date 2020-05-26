package com.github.javaparser;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
import com.github.javaparser.utils.LineEnding;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class LineEndingProcessorTest {

    // TODO: Add more tests outside the "happy path" (e.g. mixed EOL, no EOL, etc.)

    /*
     * This test case must prevent an UnsupportedOperation Removed throwed by LexicalPreservation when we try to replace an expression
     */
    public void doTest(LineEnding eol) {

        final String original = "" +
                "    public class Foo { //comment" + eol.toRawString() +
                "        private String a;" + eol +
                "        private String b;" + eol +
                "        private String c;" + eol +
                "        private String d;" + eol +
                "    }";

        // Note: Expect the platform's EOL character when printing
        String expected = "" +
                "    public class Foo { //comment" + eol +
                "    private String newField;" + eol +
                "    " + eol +
                "    private String a;" + eol +
                "        private String b;" + eol +
                "        private String c;" + eol +
                "        private String d;" + eol +
                "    }";


        CompilationUnit cu = StaticJavaParser.parse(original);
        LexicalPreservingPrinter.setup(cu);

        // create a new field declaration
        VariableDeclarator variable = new VariableDeclarator(new ClassOrInterfaceType("String"), "newField");
        FieldDeclaration fd = new FieldDeclaration(new NodeList(Modifier.privateModifier()), variable);
        Optional<ClassOrInterfaceDeclaration> cd = cu.findFirst(ClassOrInterfaceDeclaration.class);

        // add the new variable
        cd.get().getMembers().addFirst(fd);

        // should be printed like this
        System.out.println("\n\nOriginal:\n" + original);
        System.out.println("\n\nExpected:\n" + expected);

        // but the result is
        final String actual = LexicalPreservingPrinter.print(cu);
        System.out.println("\n\nActual:\n" + actual);


        // The LineEndingProcessingProvider sets the line ending to the root node.
        // Child nodes should then "inherit" then line ending style.
        LineEnding lineEnding_cu = cu.getLineEndingStyle();
        LineEnding lineEnding_fd = fd.getLineEndingStyle();

        System.out.println("lineEnding_cu.describe() = " + lineEnding_cu.describe());
        System.out.println("lineEnding_fd.describe() = " + lineEnding_fd.describe());

        // Assert that it has been detected and injected correctly.
        LineEnding detectedLineEnding = LineEnding.detect(actual);
        assertEquals(eol, detectedLineEnding);
        assertEquals(eol, lineEnding_cu);
        assertEquals(eol, lineEnding_fd);

        // The line ending data is injected at the root node, thus should only exist there.
        assertTrue(cu.containsData(Node.LINE_ENDING_KEY), "Expected the processor provider to have set the data on the root node.");
        assertFalse(fd.containsData(Node.LINE_ENDING_KEY), "Expected the line ending value to have been inherited, not set directly");

    }

    @Test
    public void testWithCr() {
        doTest(LineEnding.CR);
    }

    @Test
    public void testWithCrLf() {
        doTest(LineEnding.CRLF);
    }

    @Test
    public void testWithLf() {
        doTest(LineEnding.LF);
    }


    // TODO: Test for textblocks

}
