package ataxx;

import static ataxx.PieceColor.*;

/** A Player that receives its moves from its Game's getMoveCmnd method.
 *  @author Dawon Lee
 */
class Manual extends Player {

    /** A Player that will play MYCOLOR on GAME, taking its moves from
     *  GAME. */
    Manual(Game game, PieceColor myColor) {
        super(game, myColor);
    }

    @Override
    Move myMove() {
        String prompt;
        if (myColor().equals(RED)) {
            prompt = "Red:";
        } else {
            prompt = "Blue:";
        }
        Command m = game().getMoveCmnd(prompt);
        String[] op = m.operands();
        char col0 = op[0].charAt(0);
        char row0 = op[1].charAt(0);
        char col1 = op[2].charAt(0);
        char row1 = op[3].charAt(0);
        return Move.move(col0, row0, col1, row1);
    }
}

