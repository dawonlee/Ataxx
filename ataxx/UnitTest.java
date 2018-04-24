package ataxx;

import org.junit.Test;
import ucb.junit.textui;

import static ataxx.Command.Type.AUTO;
import static ataxx.Command.Type.LOAD;
import static ataxx.Command.Type.MANUAL;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/** The suite of all JUnit tests for the ataxx package.
 *  @author Dawon Lee
 */
public class UnitTest {
    private static final String[]
            GAME3 = {"a7-a5", "a1-a2",
                "a5-a3", "a1-b1", "a3-a4",
                "a1-b3", "g1-e1", "a2-a1",
                "e1-c1", "a1-c2"};
    private static final String[]
            GAME4 = {"a7-a6", "a1-b1",
                "a6-b7", "b1-c1", "a6-a5",
                "a1-a2", "a5-c7", "c1-d1",
                "c7-d6", "d1-e1", "d6-e7",
                "e1-f1", "e7-f7", "c1-d2",
                "c7-d7", "a1-b2", "a6-a5"};

    private static void makeMoves(Board b, String[] moves) {
        for (String s : moves) {
            b.makeMove(s.charAt(0), s.charAt(1),
                    s.charAt(3), s.charAt(4));
        }
    }

    static void check(String cmnd, Command.Type type, String... operands) {
        Command c = Command.parseCommand(cmnd);
        assertEquals("Wrong type of command identified", type,
                c.commandType());
        if (operands.length == 0) {
            assertEquals("Command has wrong number of operands", 0,
                    c.operands() == null ? 0 : c.operands().length);
        } else {
            assertArrayEquals("Operands extracted incorrectly",
                    operands, c.operands());
        }
    }

    /** Run the JUnit tests in this package. Add xxxTest.class entries to
     *  the arguments of runClasses to run other JUnit tests. */
    public static void main(String[] ignored) {
        textui.runClasses(CommandTest.class, MoveTest.class,
                          BoardTest.class);
        testUndo2();
        testUndo3();
        testLoad();
        testAUTO2();
        testManual();
    }

    @Test
    public static void testUndo2() {
        Board b = new Board();
        b.setBlock('b', '2');
        b.setBlock('c', '3');
        b.setBlock('c', '4');
        Board b1 = new Board(b);

        makeMoves(b, GAME3);

        for (int i = 0; i < 10; i++) {
            b.undo();
        }
        assertEquals(b1, b);
    }

    @Test public static void testUndo3() {
        Board b = new Board();
        Board b1 = new Board(b);

        makeMoves(b, GAME4);
        for (int i = 0; i < GAME4.length; i++) {
            b.undo();
        }
        assertEquals(b, b1);
    }

    @Test public static void testLoad() {
        check("load staff-src/test08.in", LOAD, "staff-src/test08.in");
    }

    @Test public static void testAUTO2() {
        check("auTo red", AUTO, "red");
    }

    @Test public static void testManual() {
        check("ManUal red", MANUAL, "red");
    }
}


