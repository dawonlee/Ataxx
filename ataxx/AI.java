package ataxx;

import java.util.ArrayList;
import static ataxx.PieceColor.*;
import static java.lang.Math.min;
import static java.lang.Math.max;

/** A Player that computes its own moves.
 *  @author Dawon Lee
 *  Refereced Lecture slides 22 and wikipedia minimax
 *  pseudo code.
 */
class AI extends Player {

    /** Maximum minimax search depth before going to static evaluation. */
    private static final int MAX_DEPTH = 4;
    /** A position magnitude indicating a win (for red if positive, blue
     *  if negative). */
    private static final int WINNING_VALUE = Integer.MAX_VALUE - 1;
    /** A magnitude greater than a normal value. */
    private static final int INFTY = Integer.MAX_VALUE;

    /** A new AI for GAME that will play MYCOLOR. */
    AI(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        if (!board().canMove(myColor())) {
            return Move.pass();
        }
        Move move = findMove();
        return move;
    }

    /** Return a move for me from the current position, assuming there
     *  is a move. */
    private Move findMove() {
        Board b = new Board(board());
        if (myColor() == RED) {
            findMove(b, MAX_DEPTH, true, 1, -INFTY, INFTY);
        } else {
            findMove(b, MAX_DEPTH, true, -1, -INFTY, INFTY);
        }
        return _lastFoundMove;
    }

    /** Used to communicate best moves found by findMove, when asked for. */
    private Move _lastFoundMove;

    /** Find a move from position BOARD and return its value, recording
     *  the move found in _lastFoundMove iff SAVEMOVE. The move
     *  should have maximal value or have value >= BETA if SENSE==1,
     *  and minimal value or value <= ALPHA if SENSE==-1. Searches up to
     *  DEPTH levels before using a static estimate. */
    private int findMove(Board board, int depth, boolean saveMove, int sense,
                         int alpha, int beta) {
        ArrayList<Move> allLegMoves = legalMoves(board, board.whoseMove());
        if (depth == 0 || board.gameOver()
                || allLegMoves.isEmpty()) {
            return staticScore(board);
        }
        if (saveMove) {
            _lastFoundMove = allLegMoves.get(0);
        }
        if (sense == 1) {
            for (Move m : allLegMoves) {
                char col0 = m.col0();
                char row0 = m.row0();
                char col1 = m.col1();
                char row1 = m.row1();
                board.makeMove(col0, row0, col1, row1);
                int val = findMove(board, depth - 1, false, -1, alpha, beta);
                alpha = max(alpha, val);
                board.undo();
                if (beta <= alpha) {
                    break;
                }
            }
            return alpha;
        } else if (sense == -1) {
            for (Move m : allLegMoves) {
                char col0 = m.col0();
                char row0 = m.row0();
                char col1 = m.col1();
                char row1 = m.row1();
                board.makeMove(col0, row0, col1, row1);
                int val = findMove(board, depth - 1, false, 1, alpha, beta);
                beta = min(beta, val);
                board.undo();
                if (beta <= alpha) {
                    break;
                }
            }
            return beta;
        }
        return 0;
    }

    /** Returns a list of all possible moves of WHO on the BOARD.*/
    private ArrayList<Move> legalMoves(Board board, PieceColor who) {
        int numB = board.bluePieces();
        int numR = board.redPieces();
        ArrayList<Move> allLeglMoves = new ArrayList<>();
        if (numB + numR == board.SIDE * board.SIDE) {
            return allLeglMoves;
        }
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                char ci = (char) i;
                char cj = (char) j;
                if (board.get(ci, cj).equals(who)) {
                    for (int x = i - 2; x < i + 3; x++) {
                        for (int y = j - 2; y < j + 3; y++) {
                            if (x != i || y != j) {
                                char cx = (char) x;
                                char cy = (char) y;
                                Move m = Move.move(ci, cj, cx, cy);
                                if (board.legalMove(m)) {
                                    allLeglMoves.add(m);
                                }
                            }
                        }
                    }
                }
            }
        }
        return allLeglMoves;
    }

    /** Return a heuristic value for BOARD. */
    private int staticScore(Board board) {
        int numR = board.redPieces();
        int numB = board.bluePieces();
        if (myColor() == RED) {
            return numR - numB;
        }
        return numB - numR;
    }
}
