package ataxx;

/* Author: P. N. Hilfinger, (C) 2008. */

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Stack;
import java.util.Formatter;
import java.util.Observable;

import static ataxx.PieceColor.*;
import static ataxx.GameException.error;

/** An Ataxx board.   The squares are labeled by column (a char value between
 *  'a' - 2 and 'g' + 2) and row (a char value between '1' - 2 and '7'
 *  + 2) or by linearized index, an integer described below.  Values of
 *  the column outside 'a' and 'g' and of the row outside '1' to '7' denote
 *  two layers of border squares, which are always blocked.
 *  This artificial border (which is never actually printed) is a common
 *  trick that allows one to avoid testing for edge conditions.
 *  For example, to look at all the possible moves from a square, sq,
 *  on the normal board (i.e., not in the border region), one can simply
 *  look at all squares within two rows and columns of sq without worrying
 *  about going off the board. Since squares in the border region are
 *  blocked, the normal logic that prevents moving to a blocked square
 *  will apply.
 *
 *  For some purposes, it is useful to refer to squares using a single
 *  integer, which we call its "linearized index".  This is simply the
 *  number of the square in row-major order (counting from 0).
 *
 *  Moves on this board are denoted by Moves.
 *  @author Dawon Lee
 */
class Board extends Observable {

    /** Number of squares on a side of the board. */
    static final int SIDE = 7;
    /** Length of a side + an artificial 2-deep border region. */
    static final int EXTENDED_SIDE = SIDE + 4;

    /** Number of non-extending moves before game ends. */
    static final int JUMP_LIMIT = 25;

    /** A new, cleared board at the start of the game. */
    Board() {
        _board = new PieceColor[EXTENDED_SIDE * EXTENDED_SIDE];
        clear();
    }

    /** A copy of B. */
    Board(Board b) {
        int extSide = EXTENDED_SIDE;
        _board = b._board.clone();
        clear();
        _allMoves.addAll(b.allMoves());
        _numReds = b.redPieces();
        _numBlues = b.bluePieces();
        _numJumps = b.numJumps();
        _whoseMove = b.whoseMove();
        _undoC.addAll(b._undoC);
        _undoI.addAll(b._undoI);
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                char ci = (char) i;
                char cj = (char) j;
                _board[index(ci, cj)] = b._board[index(ci, cj)];
            }
        }
    }

    /** Return the linearized index of square COL ROW. */
    static int index(char col, char row) {
        return (row - '1' + 2) * EXTENDED_SIDE + (col - 'a' + 2);
    }

    /** Return the linearized index of the square that is DC columns and DR
     *  rows away from the square with index SQ. */
    static int neighbor(int sq, int dc, int dr) {
        return sq + dc + dr * EXTENDED_SIDE;
    }

    /** Clear me to my starting state, with pieces in their initial
     *  positions and no blocks. */
    void clear() {
        _whoseMove = RED;
        _numReds = 2;
        _numBlues = 2;
        _numJumps = 0;
        _allMoves = new ArrayList<Move>();
        _undoI = new Stack<Integer>();
        _undoC = new Stack<PieceColor>();
        for (int i = 'a' - 2; i <= 'g' + 2; i++) {
            for (int j = '1' - 2; j <= '7' + 2; j++) {
                char ci = (char) i;
                char cj = (char) j;
                set(ci, cj, BLOCKED);
            }
        }
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                char ci = (char) i;
                char cj = (char) j;
                set(ci, cj, EMPTY);
            }
        }
        set('a', '1', BLUE);
        set('a', '7', RED);
        set('g', '1', RED);
        set('g', '7', BLUE);
        setChanged();
        notifyObservers();
    }

    /** Return true iff the game is over: i.e., if neither side has
     *  any moves, if one side has no pieces, or if there have been
     *  MAX_JUMPS consecutive jumps without intervening extends. */
    boolean gameOver() {
        if (_numBlues == 0
                || _numReds == 0
                || _numJumps >= JUMP_LIMIT
                || (!canMove(RED) && !canMove(BLUE))) {
            return true;
        }
        return false;
    }

    /** Return number of red pieces on the board. */
    int redPieces() {
        return numPieces(RED);
    }

    /** Return number of blue pieces on the board. */
    int bluePieces() {
        return numPieces(BLUE);
    }

    /** Return number of COLOR pieces on the board. */
    int numPieces(PieceColor color) {
        if (color.equals(RED)) {
            return _numReds;
        } else if (color.equals(BLUE)) {
            return _numBlues;
        }
        return 0;
    }

    /** Increment numPieces(COLOR) by K. */
    private void incrPieces(PieceColor color, int k) {
        if (color.equals(RED)) {
            _numReds += k;
        } else if (color.equals(BLUE)) {
            _numBlues += k;
        }
    }

    /** The current contents of square CR, where 'a'-2 <= C <= 'g'+2, and
     *  '1'-2 <= R <= '7'+2.  Squares outside the range a1-g7 are all
     *  BLOCKED.  Returns the same value as get(index(C, R)). */
    PieceColor get(char c, char r) {
        return _board[index(c, r)];
    }

    /** Return the current contents of square with linearized index SQ. */
    PieceColor get(int sq) {
        return _board[sq];
    }

    /** Set get(C, R) to V, where 'a' <= C <= 'g', and
     *  '1' <= R <= '7'. */
    private void set(char c, char r, PieceColor v) {
        set(index(c, r), v);
    }

    /** Set square with linearized index SQ to V.  This operation is
     *  undoable. */
    private void set(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Set square at C R to V (not undoable). */
    private void unrecordedSet(char c, char r, PieceColor v) {
        _board[index(c, r)] = v;
    }

    /** Set square at linearized index SQ to V (not undoable). */
    private void unrecordedSet(int sq, PieceColor v) {
        _board[sq] = v;
    }

    /** Return true iff MOVE is legal on the current board. */
    boolean legalMove(Move move) {
        if (move == null) {
            return false;
        }
        if (!move.isPass()) {
            if (!get(move.toIndex()).equals(EMPTY)) {
                return false;
            }
            if (!move.isExtend() && !move.isJump()) {
                return false;
            }
            PieceColor origin = get(move.fromIndex());
            if (origin.equals(EMPTY)
                    || origin.equals(BLOCKED)
                    || !origin.equals(_whoseMove)) {
                return false;
            }
        }
        return true;
    }

    /** Return true iff player WHO can move, ignoring whether it is
     *  that player's move and whether the game is over. */
    boolean canMove(PieceColor who) {
        for (int i = 'a'; i <= 'g'; i++) {
            for (int j = '1'; j <= '7'; j++) {
                char ci = (char) i;
                char cj = (char) j;
                if (get(ci, cj).equals(who)) {
                    for (int x = -2; x < 3; x++) {
                        for (int y = -2; y < 3; y++) {
                            int nei = neighbor(index(ci, cj) , x, y);
                            if (_board[nei].equals(EMPTY)) {
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    /** Return the color of the player who has the next move.  The
     *  value is arbitrary if gameOver(). */
    PieceColor whoseMove() {
        return _whoseMove;
    }

    /** Return total number of moves and passes since the last
     *  clear or the creation of the board. */
    int numMoves() {
        return _allMoves.size();
    }

    /** Return number of non-pass moves made in the current game since the
     *  last extend move added a piece to the board (or since the
     *  start of the game). Used to detect end-of-game. */
    int numJumps() {
        return _numJumps;
    }

    /** Perform the move C0R0-C1R1, or pass if C0 is '-'.  For moves
     *  other than pass, assumes that legalMove(C0, R0, C1, R1). */
    void makeMove(char c0, char r0, char c1, char r1) {
        if (c0 == '-') {
            makeMove(Move.pass());
        } else {
            makeMove(Move.move(c0, r0, c1, r1));
        }
    }

    /** Make the MOVE on this Board, assuming it is legal. */
    void makeMove(Move move) {
        assert legalMove(move);
        if (move.isPass()) {
            pass();
            return;
        }
        char col0 = move.col0();
        char row0 = move.row0();
        char col1 = move.col1();
        char row1 = move.row1();
        PieceColor p = get(col0, row0);
        if (canMove(p) && !gameOver()) {
            _allMoves.add(move);
            startUndo();
            int k = 0;
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    char neiCol = (char) (col1 + i);
                    char neiRow = (char) (row1 + j);
                    PieceColor nei = get(neiCol, neiRow);
                    if (nei.isPiece()) {
                        if (!nei.equals(p)) {
                            addUndo(index(neiCol, neiRow), nei);
                            set(neiCol, neiRow, p);
                            k++;
                        }
                    }
                }
            }
            if (move.isJump()) {
                incrPieces(p, k);
                incrPieces(p.opposite(), -k);
                set(col0, row0, EMPTY);
                addUndo(index(col0, row0), p);
            } else if (move.isExtend()) {
                incrPieces(p, k + 1);
                incrPieces(p.opposite(), -k);
            }
            addUndo(index(col1, row1), EMPTY);
            set(col1, row1, p);
        }
        if (move.isExtend()) {
            _numJumps = 0;
        } else {
            _numJumps++;
        }
        PieceColor opponent = _whoseMove.opposite();
        _whoseMove = opponent;
        setChanged();
        notifyObservers();
    }

    /** Update to indicate that the current player passes, assuming it
     *  is legal to do so.  The only effect is to change whoseMove(). */
    void pass() {
        assert !canMove(_whoseMove) && numPieces(_whoseMove) != 0;
        _numJumps++;
        _whoseMove = _whoseMove.opposite();
        setChanged();
        notifyObservers();
    }

    /** Undo the last move. */
    void undo() {
        int last = _allMoves.size() - 1;
        Move move = _allMoves.get(last);
        PieceColor dstCol = get(move.col1(), move.row1());
        int numEmp = 0;
        int numCol = 0;
        int numOpCol = 0;
        while (_undoI.peek() != null) {
            int nextSq = _undoI.pop();
            PieceColor nextCol = _undoC.pop();
            set(nextSq, nextCol);
            if (nextCol.equals(EMPTY)) {
                numEmp++;
            } else if (nextCol.equals(dstCol)) {
                numCol++;
            } else if (nextCol.equals(dstCol.opposite())) {
                numOpCol++;
            }
        }
        _undoC.pop();
        _undoI.pop();
        incrPieces(dstCol, numCol - numOpCol - numEmp);
        incrPieces(dstCol.opposite(), numOpCol);
        if (move.isJump() || move.isExtend()) {
            _numJumps--;
        }
        _whoseMove = _whoseMove.opposite();
        _allMoves.remove(last);
        setChanged();
        notifyObservers();
    }

    /** Indicate beginning of a move in the undo stack. */
    private void startUndo() {
        _undoI.push(null);
        _undoC.push(null);
    }

    /** Add an undo action for changing SQ to NEWCOLOR on current
     *  board. */
    private void addUndo(int sq, PieceColor newColor) {
        _undoI.push(sq);
        _undoC.push(newColor);
    }

    /** Return true iff it is legal to place a block at C R. */
    boolean legalBlock(char c, char r) {
        int sq = index(c, r);
        if (get(c, r).isPiece()
                || sq == index('a', '1')
                || sq == index('a', '7')
                || sq == index('g', '1')
                || sq == index('g', '7')) {
            return false;
        }
        return true;
    }

    /** Return true iff it is legal to place a block at CR. */
    boolean legalBlock(String cr) {
        return legalBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Set a block on the square C R and its reflections across the middle
     *  row and/or column, if that square is unoccupied and not
     *  in one of the corners. Has no effect if any of the squares is
     *  already occupied by a block.  It is an error to place a block on a
     *  piece. */
    void setBlock(char c, char r) {
        if (!legalBlock(c, r)) {
            throw error("illegal block placement");
        }
        char newR = (char) ('1' + '7' - r);
        char newC = (char) ('a' + 'g' - c);
        if (!legalBlock(c, newR)
                || !legalBlock(newC, r)
                || !legalBlock(newC, newR)) {
            throw error("illegal block");
        }
        if (!get(c, r).equals(BLOCKED)) {
            set(c, r, BLOCKED);
            set(c, newR, BLOCKED);
            set(newC, r, BLOCKED);
            set(newC, newR, BLOCKED);
        }
        setChanged();
        notifyObservers();
    }

    /** Place a block at CR. */
    void setBlock(String cr) {
        setBlock(cr.charAt(0), cr.charAt(1));
    }

    /** Return a list of all moves made since the last clear (or start of
     *  game). */
    List<Move> allMoves() {
        return _allMoves;
    }

    @Override
    public String toString() {
        return toString(false);
    }

    /* .equals used only for testing purposes. */
    @Override
    public boolean equals(Object obj) {
        Board other = (Board) obj;
        return Arrays.equals(_board, other._board);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(_board);
    }

    /** Return a text depiction of the board (not a dump).  If LEGEND,
     *  supply row and column numbers around the edges. */
    String toString(boolean legend) {
        Formatter out = new Formatter();
        if (legend) {
            out.format("   a b c d e f g%n");
        }
        for (int r = '7'; r > '0'; r--) {
            out.format("  ");
            if (legend) {
                out.format("%d", r);
            }
            for (int c = 'a'; c <= 'g'; c++) {
                PieceColor elem = get((char) c, (char) r);
                if (elem.equals(EMPTY)) {
                    out.format(" -");
                } else if (elem.equals(BLOCKED)) {
                    out.format(" X");
                } else if (elem.equals(BLUE)) {
                    out.format(" b");
                } else {
                    out.format(" r");
                }
            }
            out.format("%n");
        }
        return out.toString();
    }

    /** For reasons of efficiency in copying the board,
     *  we use a 1D array to represent it, using the usual access
     *  algorithm: row r, column c => index(r, c).
     *
     *  Next, instead of using a 7x7 board, we use an 11x11 board in
     *  which the outer two rows and columns are blocks, and
     *  row 2, column 2 actually represents row 0, column 0
     *  of the real board.  As a result of this trick, there is no
     *  need to special-case being near the edge: we don't move
     *  off the edge because it looks blocked.
     *
     *  Using characters as indices, it follows that if 'a' <= c <= 'g'
     *  and '1' <= r <= '7', then row c, column r of the board corresponds
     *  to board[(c -'a' + 2) + 11 (r - '1' + 2) ], or by a little
     *  re-grouping of terms, board[c + 11 * r + SQUARE_CORRECTION]. */
    private final PieceColor[] _board;

    /** Player that is on move.*/
    private PieceColor _whoseMove;

    /** The number of red pieces.*/
    private int _numReds;

    /** The number of blue pieces.*/
    private int _numBlues;

    /** The list of all moves.*/
    private List<Move> _allMoves;

    /** The number of consecutive jumps so far.*/
    private int _numJumps;

    /** The stack of index of changed pieces for undo.*/
    private Stack<Integer> _undoI;

    /** The stack of colors of changed pieces for undo.*/
    private Stack<PieceColor> _undoC;

}
