package ataxx;

/* Author: P. N. Hilfinger */

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Random;
import java.util.function.Consumer;

import static ataxx.PieceColor.*;
import static ataxx.Game.State.*;
import static ataxx.Command.Type.*;
import static ataxx.GameException.error;

/** Controls the play of the game.
 *  @author Dawon Lee
 */
class Game {

    /** States of play. */
    static enum State {
        SETUP, PLAYING, FINISHED;
    }

    /** A new Game, using BOARD to play on, reading initially from
     *  BASESOURCE and using REPORTER for error and informational messages. */
    Game(Board board, CommandSource baseSource, Reporter reporter) {
        _inputs.addSource(baseSource);
        _board = board;
        _reporter = reporter;
        _state = SETUP;
    }

    /** Run a session of Ataxx gaming.  Use an AtaxxGUI iff USEGUI. */
    void process(boolean useGUI) {
        Player red, blue;
        while (true) {
            doClear(null);
            while (_state == SETUP) {
                doCommand();
            }
            if (redIsAI) {
                red = new AI(this, RED);
            } else {
                red = new Manual(this, RED);
            }
            if (blueIsAI) {
                blue = new AI(this, BLUE);
            } else {
                blue = new Manual(this, BLUE);
            }
            _state = PLAYING;
            while (_state != SETUP && !_board.gameOver()) {
                if (_state == PLAYING) {
                    try {
                        Move move;
                        if (_board.whoseMove().equals(RED)) {
                            move = red.myMove();
                            if (redIsAI) {
                                if (move.isPass()) {
                                    _reporter.outcomeMsg("Red passes.");
                                } else {
                                    reportMove(rms, move.col0(),
                                            move.row0(), move.col1(),
                                            move.row1());
                                }
                            }
                        } else {
                            move = blue.myMove();
                            if (blueIsAI) {
                                if (move.isPass()) {
                                    _reporter.outcomeMsg("Blue passes.");
                                } else {
                                    reportMove(bms, move.col0(),
                                            move.row0(), move.col1(),
                                            move.row1());
                                }
                            }
                        }
                        _board.makeMove(move);
                    } catch (AssertionError ae) {
                        _reporter.errMsg("Illegal Move.");
                    }
                }
            }
            if (_state != SETUP) {
                reportWinner();
            }
            while (_state == FINISHED) {
                doCommand();
            }
        }
    }

    /** Return a view of my game board that should not be modified by
     *  the caller. */
    Board board() {
        return _board;
    }

    /** Perform the next command from our input source. */
    void doCommand() {
        try {
            Command cmnd =
                Command.parseCommand(_inputs.getLine("ataxx: "));
            _commands.get(cmnd.commandType()).accept(cmnd.operands());
        } catch (GameException excp) {
            _reporter.errMsg(excp.getMessage());
        }
    }

    /** Read and execute commands until encountering a move or until
     *  the game leaves playing state due to one of the commands. Return
     *  the terminating move command, or null if the game first drops out
     *  of playing mode. If appropriate to the current input source, use
     *  PROMPT to prompt for input. */
    Command getMoveCmnd(String prompt) {
        while (_state == PLAYING) {
            try {
                Command cmnd = Command.parseCommand(_inputs.getLine(prompt));
                Command.Type ty = cmnd.commandType();
                if (ty.equals(PIECEMOVE)) {
                    return cmnd;
                } else {
                    _commands.get(ty).accept(cmnd.operands());
                }
            } catch (GameException excp) {
                _reporter.errMsg(excp.getMessage());
            }
        }
        return null;
    }

    /** Return random integer between 0 (inclusive) and MAX>0 (exclusive). */
    int nextRandom(int max) {
        return _randoms.nextInt(max);
    }

    /** Report a move, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportMove(String format, Object... args) {
        _reporter.moveMsg(format, args);
    }

    /** Report an error, using a message formed from FORMAT and ARGS as
     *  for String.format. */
    void reportError(String format, Object... args) {
        _reporter.errMsg(format, args);
    }

    /* Command Processors */

    /** Perform the command 'auto OPERANDS[0]'. */
    void doAuto(String[] operands) {
        checkState("auto", SETUP);
        switch (operands[0].toLowerCase()) {
        case "red":
            redIsAI = true;
            break;
        case "blue":
            blueIsAI = true;
            break;
        default:
            break;
        }
    }

    /** Perform a 'help' command. */
    void doHelp(String[] unused) {
        InputStream helpIn =
            Game.class.getClassLoader().getResourceAsStream("ataxx/help.txt");
        if (helpIn == null) {
            System.err.println("No help available.");
        } else {
            try {
                BufferedReader r
                    = new BufferedReader(new InputStreamReader(helpIn));
                while (true) {
                    String line = r.readLine();
                    if (line == null) {
                        break;
                    }
                    System.out.println(line);
                }
                r.close();
            } catch (IOException e) {
                /* Ignore IOException */
            }
        }
    }

    /** Perform the command 'load OPERANDS[0]'. */
    void doLoad(String[] operands) {
        try {
            FileReader reader = new FileReader(operands[0]);
            ReaderSource rs = new ReaderSource(reader, false);
            _inputs.addSource(rs);
        } catch (IOException e) {
            throw error("Cannot open file %s", operands[0]);
        }
    }

    /** Perform the command 'manual OPERANDS[0]'. */
    void doManual(String[] operands) {
        checkState("manual", SETUP);
        switch (operands[0].toLowerCase()) {
        case "red":
            redIsAI = false;
            break;
        case "blue":
            blueIsAI = false;
            break;
        default:
            break;
        }
    }

    /** Exit the program. */
    void doQuit(String[] unused) {
        System.exit(0);
    }

    /** Perform the command 'start'. */
    void doStart(String[] unused) {
        checkState("start", SETUP);
        _state = PLAYING;
    }

    /** Perform the move OPERANDS[0]. */
    void doMove(String[] operands) {
        checkState("move", SETUP);
        try {
            char col0 = operands[0].charAt(0);
            char row0 = operands[1].charAt(0);
            char col1 = operands[2].charAt(0);
            char row1 = operands[3].charAt(0);
            _board.makeMove(col0, row0, col1, row1);
        } catch (AssertionError ge) {
            _reporter.errMsg("Illegal Move.");
        }
    }

    /** Cause current player to pass. */
    void doPass(String[] unused) {
        checkState("pass", SETUP, PLAYING);
        try {
            _board.pass();
        } catch (AssertionError e) {
            _reporter.errMsg("Player can move, so may not pass.");
        }

    }

    /** Perform the command 'clear'. */
    void doClear(String[] unused) {
        _board.clear();
        _state = SETUP;
    }

    /** Perform the command 'dump'. */
    void doDump(String[] unused) {
        String msg = "===%n";
        msg += _board.toString();
        msg += "===";
        _reporter.outcomeMsg(msg);
    }

    /** Execute 'seed OPERANDS[0]' command, where the operand is a string
     *  of decimal digits. Silently substitutes another value if
     *  too large. */
    void doSeed(String[] operands) {
        checkState("seed", SETUP);
        int seed = Integer.parseInt(operands[0]) % largeInt;
        _randoms.setSeed(seed);
    }

    /** Execute the command 'block OPERANDS[0]'. */
    void doBlock(String[] operands) {
        checkState("block", SETUP);
        char c = operands[0].charAt(0);
        char r = operands[0].charAt(1);
        _board.setBlock(c, r);
    }

    /** Execute the artificial 'error' command. */
    void doError(String[] unused) {
        throw error("Command not understood");
    }

    /** Report the outcome of the current game. */
    void reportWinner() {
        int numR = _board.redPieces();
        int numB = _board.bluePieces();
        String msg = "";
        if (numR > numB) {
            msg = "Red wins.";
        } else if (numB > numR) {
            msg = "Blue wins.";
        } else {
            msg = "Draw.";
        }
        _state = FINISHED;
        _reporter.outcomeMsg(msg.trim());
    }

    /** Check that game is currently in one of the states STATES, assuming
     *  CMND is the command to be executed. */
    private void checkState(Command cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd.commandType());
    }

    /** Check that game is currently in one of the states STATES, using
     *  CMND in error messages as the name of the command to be executed. */
    private void checkState(String cmnd, State... states) {
        for (State s : states) {
            if (s == _state) {
                return;
            }
        }
        throw error("'%s' command is not allowed now.", cmnd);
    }

    /** Mapping of command types to methods that process them. */
    private final HashMap<Command.Type, Consumer<String[]>> _commands =
        new HashMap<>();

    {
        _commands.put(AUTO, this::doAuto);
        _commands.put(BLOCK, this::doBlock);
        _commands.put(CLEAR, this::doClear);
        _commands.put(DUMP, this::doDump);
        _commands.put(HELP, this::doHelp);
        _commands.put(MANUAL, this::doManual);
        _commands.put(PASS, this::doPass);
        _commands.put(PIECEMOVE, this::doMove);
        _commands.put(SEED, this::doSeed);
        _commands.put(START, this::doStart);
        _commands.put(LOAD, this::doLoad);
        _commands.put(QUIT, this::doQuit);
        _commands.put(ERROR, this::doError);
        _commands.put(EOF, this::doQuit);
    }

    /** Input source. */
    private final CommandSources _inputs = new CommandSources();
    /** My board. */
    private Board _board;
    /** Current game state. */
    private State _state;
    /** Used to send messages to the user. */
    private Reporter _reporter;
    /** Source of pseudo-random numbers (used by AIs). */
    private Random _randoms = new Random();
    /** True if the type of red player is AI.*/
    private boolean redIsAI = false;
    /** True if the type of blue player is AI.*/
    private boolean blueIsAI = true;
    /** Large integer size.*/
    private final int largeInt = 9000;
    /** String format for red moves.*/
    private final String rms = "Red moves %s%s-%s%s.";
    /** String format for blue moves.*/
    private final String bms = "Blue moves %s%s-%s%s.";
}
