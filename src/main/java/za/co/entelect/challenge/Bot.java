package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.CellType;
import za.co.entelect.challenge.enums.Direction;

import java.util.*;
import java.util.stream.Collectors;

public class Bot {

    private Random random;
    private GameState gameState;
    private Opponent opponent;
    private MyPlayer player;
    private MyWorm currentWorm;

    public Bot(Random random, GameState gameState) {
        this.random = random;
        this.gameState = gameState;
        this.opponent = gameState.opponents[0];
        this.currentWorm = getCurrentWorm(gameState);
        this.player = gameState.myPlayer;
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms).filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst().get();
    }
    // Cek x1 between x2 sama x3 apa ga

    private boolean between(int x1, int x2, int x3) {
        return (x1 >= x3 && x1 <= x2) || (x1 <= x3 && x1 >= x2);
    }

    private boolean changeWorm(){
        for(MyWorm myWorm : player.worms){
            if(myWorm.roundsUntilUnfrozen == 0 && myWorm.health > 0){
                currentWorm = myWorm;
                return true;
            }
        }

        return false;
    }

    public Command run() {
        Cell chosenCell;
        ArrayList<Worm> enemyWorms;
        boolean selectNewWorm = false;

//        Jika cell yang ditempati worm bertipe lava maka worm akan pindah
        if(gameState.map[currentWorm.position.y][currentWorm.position.x].type == CellType.LAVA){
            chosenCell = escapeLava();
        }else{
//            Jika currentWorm frozen (tidak bisa melakukan command) dan remainingwormselections > 0 --> ganti worm
            if (currentWorm.roundsUntilUnfrozen > 0 && player.remainingWormSelections > 0) selectNewWorm = changeWorm();

//            Jika id Worm = 2, banana bomb belum habis, dan terdapat worm lawan di range banana bomb maka banana bomb dilemparkan
            if (currentWorm.id == 2 && currentWorm.bananaBomb.count > 0) {
                Cell bananaTarget = getBananaTarget();

                if (bananaTarget != null) {
                    if (selectNewWorm)
                        return new SelectCommand(currentWorm.id, new BananaCommand(bananaTarget.x, bananaTarget.y));
                    return new BananaCommand(bananaTarget.x, bananaTarget.y);
                }

//            Jika id Worm = 3, snowball belum habis, dan terdapat worm lawan di range snowball yg tidak beku maka snowball dilemparkan
            } else if (currentWorm.id == 3 && currentWorm.snowball.count > 0) { //Worm 3 --> Worm yang punya snowball
                Cell snowballTarget = getSnowballTarget();

                if (snowballTarget != null) {
                    if (selectNewWorm)
                        return new SelectCommand(currentWorm.id, new SnowballCommand(snowballTarget.x, snowballTarget.y));
                    return new SnowballCommand(snowballTarget.x, snowballTarget.y);
                }
            }

//          Mencari apakah ada worm lawan di shooting range
            enemyWorms = getFirstWormInRange();

//          Mencari apakah health pack masih ada di peta
            Cell healthPackCell = searchPowerUp();

//            Jika ada worm lawan di shooting range, maka tembak worm tersebut
            if (enemyWorms.size() > 0) {
                Direction direction = resolveDirection(currentWorm.position, getPositionWormMinHealth(enemyWorms));
                if (!isMyWormInShootingRange(direction)) {
                    if (selectNewWorm)
                        return new SelectCommand(currentWorm.id, new ShootCommand(direction));
                    return new ShootCommand(direction);
                }
                chosenCell = searchEmptySurrounding();

//                Jika masih ada health pack di peta, pilih cell sekitar yang menuju health pack
            } else if (healthPackCell != null) {
                chosenCell = chooseCell(getSurroundingCells(currentWorm.position.x, currentWorm.position.y),
                        healthPackCell.x, healthPackCell.y);
            } else {
                // Mencari worm enemy yang paling deket dari semua worm pemain
                Worm closestEnemy = getClosestWorm();

//                Memilih cell yang menuju ke worm lawan yang telah dipilih
                chosenCell = chooseCell(getSurroundingCells(currentWorm.position.x, currentWorm.position.y),
                        closestEnemy.position.x, closestEnemy.position.y);
            }
        }

        if(chosenCell != null) {
//            Jika cell terpilih bertipe AIR --> move
            if (chosenCell.type == CellType.AIR) {
                if (selectNewWorm)
                    return new SelectCommand(currentWorm.id, new MoveCommand(chosenCell.x, chosenCell.y));
                return new MoveCommand(chosenCell.x, chosenCell.y);
            }
//            Jika cell terpilih bertipe DIRT --> dig
            else if (chosenCell.type == CellType.DIRT) {
                if (selectNewWorm)
                    return new SelectCommand(currentWorm.id, new DigCommand(chosenCell.x, chosenCell.y));
                return new DigCommand(chosenCell.x, chosenCell.y);
            }
        }

        return new DoNothingCommand();
    }

    private ArrayList<Worm> getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range).stream().flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y)).collect(Collectors.toSet());

        ArrayList<Worm> enemyWorms = new ArrayList<>();
        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition) && enemyWorm.health > 0) {
                enemyWorms.add(enemyWorm);
            }
        }

        return enemyWorms;
    }


    private List<List<Cell>> constructFireDirectionLines(int range) {
        List<List<Cell>> directionLines = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            List<Cell> directionLine = new ArrayList<>();
            for (int directionMultiplier = 1; directionMultiplier <= range; directionMultiplier++) {

                int coordinateX = currentWorm.position.x + (directionMultiplier * direction.x);
                int coordinateY = currentWorm.position.y + (directionMultiplier * direction.y);

                if (!isValidCoordinate(coordinateX, coordinateY)) {
                    break;
                }

                if (euclideanDistance(currentWorm.position.x, currentWorm.position.y, coordinateX,
                        coordinateY) > range) {
                    break;
                }

                Cell cell = gameState.map[coordinateY][coordinateX];
                if (cell.type != CellType.AIR) {
                    break;
                }

                directionLine.add(cell);
            }
            directionLines.add(directionLine);
        }

        return directionLines;
    }

    private List<Cell> getSurroundingCells(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 1; i <= x + 1; i++) {
            for (int j = y - 1; j <= y + 1; j++) {
                // Don't include the current position
                if ((i != x || j != y) && isValidCoordinate(i, j)) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    private int euclideanDistance(int aX, int aY, int bX, int bY) {
        return (int) (Math.sqrt(Math.pow(aX - bX, 2) + Math.pow(aY - bY, 2)));
    }

    private boolean isValidCoordinate(int x, int y) {
        return x >= 0 && x < gameState.mapSize && y >= 0 && y < gameState.mapSize;
    }

    private Direction resolveDirection(Position a, Position b) {
        StringBuilder builder = new StringBuilder();

        int verticalComponent = b.y - a.y;
        int horizontalComponent = b.x - a.x;

        if(verticalComponent < 0 && horizontalComponent < 0){
            return Direction.valueOf("NW");
        } else if(verticalComponent < 0 && horizontalComponent > 0){
            return Direction.valueOf("NE");
        } else if(verticalComponent > 0 && horizontalComponent > 0){
            return Direction.valueOf("SE");
        } else if(verticalComponent > 0 && horizontalComponent < 0){
            return Direction.valueOf("SW");
        }
        if (verticalComponent < 0) {
            return Direction.valueOf("N");
        } else if (verticalComponent > 0) {
            return Direction.valueOf("S");
        }

        if (horizontalComponent < 0) {
            return Direction.valueOf("W");
        } else if (horizontalComponent > 0) {
            return Direction.valueOf("E");
        }

        return Direction.valueOf(builder.toString());
    }

    private boolean isDirt(Cell c) {
        return (c.type == CellType.DIRT);
    }

    private boolean isLava(Cell c) {
        return (c.type == CellType.LAVA);
    }

    private boolean isDeepSpace(Cell c) {
        return (c.type == CellType.DEEP_SPACE);
    }

    // Cek apakah cell diisi oleh worm lain
    private boolean isCellOccupied(Cell c) {
        Worm[] opponentWorms = opponent.worms;
        Worm[] playerWorms = player.worms;
        int i = 0;
        int j = 0;
        boolean foundOpponentWorm = false;
        boolean foundPlayerWorm = false;
        while ((i < opponentWorms.length) && (!foundOpponentWorm)) {
            if ((opponentWorms[i].position.x == c.x) && (opponentWorms[i].position.y == c.y)) {
                foundOpponentWorm = true;
            } else {
                i++;
            }
        }
        while ((j < playerWorms.length) && (!foundPlayerWorm)) {
            if ((playerWorms[j].position.x == c.x) && (playerWorms[j].position.y == c.y)) {
                foundPlayerWorm = true;
            } else {
                j++;
            }
        }
        return (foundOpponentWorm || foundPlayerWorm);
    }

    // Cek apakah cell surrounded by dirt
    private boolean isSurroundedByDirtAndLava() {
        List<Cell> surrounding = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int i = 0;
        boolean isAllDirt = true;
        while (i < surrounding.size() && (isAllDirt)) {
            if (!isDirt(surrounding.get(i)) && !isLava(surrounding.get(i))) {
                isAllDirt = false;
            } else {
                i++;
            }
        }
        return isAllDirt;
    }

    // Cek apakah cell surrounded by deep space
    private boolean isSurroundedByDeepSpace() {
        List<Cell> surrounding = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        int i = 0;
        boolean isAllDeepSpace = true;
        while (i < surrounding.size() && (isAllDeepSpace)) {
            if (!isDeepSpace(surrounding.get(i))) {
                isAllDeepSpace = false;
            } else {
                i++;
            }
        }
        return isAllDeepSpace;
    }

    // Cari posisi worm lawan yang health nya minimum
    private Position getPositionWormMinHealth(ArrayList<Worm> worms) {
        Worm wormWithMinHealth = worms.get(0);
        int i;
        for (Worm worm : worms) {
            if (worm.health < wormWithMinHealth.health) {
                wormWithMinHealth = worm;
            }
        }
        return (wormWithMinHealth.position);
    }

    //    Mencari target untuk dilempar snowball
    private Cell getSnowballTarget() {
        Cell[][] blocks = gameState.map;
        int mostWormInRange = 0;
        Cell chosenCell = null;

        for (int i = currentWorm.position.x - 5; i <= currentWorm.position.x + 5; i++) {
            for (int j = currentWorm.position.y - 5; j <= currentWorm.position.y + 5; j++) {
                if (isValidCoordinate(i, j)
                        && euclideanDistance(i, j, currentWorm.position.x, currentWorm.position.y) <= 5) {
                    List<Cell> affectedCells = getSurroundingCells(i, j);
                    affectedCells.add(blocks[j][i]);
                    int wormInRange = 0;
                    for (Cell cell : affectedCells) {
                        for (Worm enemyWorm : opponent.worms) {
                            if (enemyWorm.position.x == cell.x && enemyWorm.position.y == cell.y
                                    && enemyWorm.roundsUntilUnfrozen == 0 && enemyWorm.health > 0)
                                wormInRange++;
                        }
                        for (Worm myWorm : player.worms) {
                            if (myWorm.position.x == cell.x && myWorm.position.y == cell.y && myWorm.health > 0)
                                wormInRange = -5;
                        }
                    }
                    if (wormInRange > mostWormInRange) {
                        mostWormInRange = wormInRange;
                        chosenCell = blocks[j][i];
                    }
                }
            }
        }

        return chosenCell;
    }

//    Mencari target untuk dilempar banana bomb
    private Cell getBananaTarget() {
        Cell[][] blocks = gameState.map;
        int mostWormInRange = 0;
        Cell chosenCell = null;
        boolean wormAtCenter;

        for (int i = currentWorm.position.x - currentWorm.bananaBomb.range; i <= currentWorm.position.x + currentWorm.bananaBomb.range; i++){
            for (int j = currentWorm.position.y - currentWorm.bananaBomb.range; j <= currentWorm.position.y + currentWorm.bananaBomb.range; j++){
                wormAtCenter = false;
                if (isValidCoordinate(i, j)
                        && euclideanDistance(i, j, currentWorm.position.x, currentWorm.position.y) <= 5) {
                    List<Cell> affectedCells = getBananaAffectedCell(i, j);

                    int wormInRange = 0;
                    for (Cell cell : affectedCells) {
                        for (Worm enemyWorm : opponent.worms) {
                            if (enemyWorm.position.x == cell.x && enemyWorm.position.y == cell.y
                                    && enemyWorm.health > 0)
                                wormInRange++;
                            if (enemyWorm.position.x == i && enemyWorm.position.y == j)
                                wormAtCenter = true;
                        }
                        for (Worm myWorm : player.worms) {
                            if (myWorm.position.x == cell.x && myWorm.position.y == cell.y && myWorm.health > 0)
                                wormInRange = -5;
                        }
                    }
                    if (wormInRange > mostWormInRange) {
                        mostWormInRange = wormInRange;
                        chosenCell = blocks[j][i];
                    } else if (wormInRange == mostWormInRange && wormAtCenter) {
                        chosenCell = blocks[j][i];
                    }
                }
            }
        }

        return chosenCell;
    }

//    Mencari cell yang terdapat power up
    private Cell searchPowerUp() {
        for (Cell[] row : gameState.map) {
            for (Cell column : row) {
                if (column.powerUp != null)
                    return column;
            }
        }

        return null;
    }

    // Dari surrounding cells, dipilih cell buat move/dig
    private Cell chooseCell(List<Cell> surroundingCells, int destinationX, int destinationY) {
        boolean diagonalChosen = false;

        Cell chosenCell = surroundingCells.get(0);
        int i = 0;

        // Mencari cell yang bisa membawa ke cell tujuan
        while (i < surroundingCells.size() && !diagonalChosen) { // Karena move diagonal bakal paling efektif, kalo bisa
            // diagonal yang dipilih yang diagonal.
            Cell currentCell = surroundingCells.get(i);
            if (between(currentCell.x, destinationX, currentWorm.position.x)
                    && between(currentCell.y, destinationY, currentWorm.position.y)) {
                chosenCell = currentCell;
                if (currentCell.x != currentWorm.position.x && currentCell.y != currentWorm.position.y) {
                    diagonalChosen = true;
                }
            }
            i++;
        }
        return chosenCell;
    }

//    Mencari worm lawan yang paling dekat dengan worm pemain
    private Worm getClosestWorm() {
        Worm closestEnemy = null;
        double closestDistance = 1000;

        for (Worm enemyWorms : opponent.worms) {
            if (enemyWorms.health > 0) {
                double distance = 0;

                for (Worm myWorm : player.worms) {
                    if (myWorm.health > 0) {
                        distance += euclideanDistance(currentWorm.position.x, currentWorm.position.y,
                                enemyWorms.position.x, enemyWorms.position.y);
                    }
                }

                if (distance < closestDistance) {
                    closestDistance = distance;
                    closestEnemy = enemyWorms;
                }
            }
        }

        return closestEnemy;
    }

//    Mencari cell yang terkena damage dari banana bomb
    private List<Cell> getBananaAffectedCell(int x, int y) {
        ArrayList<Cell> cells = new ArrayList<>();
        for (int i = x - 2; i <= x + 2; i++) {
            for (int j = y - 2; j <= y + 2; j++) {
                // Don't include the current position
                if (isValidCoordinate(i, j) && euclideanDistance(i, j, x, y) <= 2) {
                    cells.add(gameState.map[j][i]);
                }
            }
        }

        return cells;
    }

    // Mengecek apakah terdapat worm pemain di arah d
    private boolean isMyWormInShootingRange(Direction d) {
        MyWorm currentWorm = getCurrentWorm(gameState);
        // direction N(0, -1), NE(1, -1), E(1, 0), SE(1, 1), S(0, 1), SW(-1, 1), W(-1,0), NW(-1, -1)
        Worm[] myWorms = player.worms;
        int i = 0;
        boolean found = false;
        while ((i < myWorms.length) && (!found)) {
            int weaponRange = currentWorm.weapon.range;
            if(myWorms[i].id != currentWorm.id && myWorms[i].health > 0){
                if ((d.name().equals("N")) && (between(myWorms[i].position.y, currentWorm.position.y - weaponRange, currentWorm.position.y) && currentWorm.position.x == myWorms[i].position.x)) {
                    found = true;
                }
                else if ((d.name().equals("E")) && (between(myWorms[i].position.x, currentWorm.position.x + weaponRange, currentWorm.position.x) && currentWorm.position.y == myWorms[i].position.y)) {
                    found = true;
                }
                else if ((d.name().equals("S")) && (between(myWorms[i].position.y, currentWorm.position.y + weaponRange, currentWorm.position.y) && currentWorm.position.x == myWorms[i].position.x)) {
                    found = true;
                }
                else if ((d.name().equals("W")) && (between(myWorms[i].position.x, currentWorm.position.x - weaponRange, currentWorm.position.x) && currentWorm.position.y == myWorms[i].position.y)) {
                    found = true;
                }
                else if ((d.name().equals("NE")) && (between(myWorms[i].position.x, currentWorm.position.x + (int) (weaponRange/Math.sqrt(2)), currentWorm.position.x) && between(myWorms[i].position.y, currentWorm.position.y - (int) (weaponRange/Math.sqrt(2)), currentWorm.position.y) && isDiagonal(currentWorm, myWorms[i]))) {
                    found = true;
                }
                else if ((d.name().equals("SE")) && (between(myWorms[i].position.x, currentWorm.position.x + (int) (weaponRange/Math.sqrt(2)), currentWorm.position.x) && between(myWorms[i].position.y, currentWorm.position.y + (int) (weaponRange/Math.sqrt(2)), currentWorm.position.y) && isDiagonal(currentWorm, myWorms[i]))) {
                    found = true;
                }
                else if ((d.name().equals("SW")) && (between(myWorms[i].position.x, currentWorm.position.x - (int) (weaponRange/Math.sqrt(2)), currentWorm.position.x) && between(myWorms[i].position.y, currentWorm.position.y + (int) (weaponRange/Math.sqrt(2)), currentWorm.position.y) && isDiagonal(currentWorm, myWorms[i]))) {
                    found = true;
                }
                else if ((d.name().equals("NW")) && (between(myWorms[i].position.x, currentWorm.position.x - (int) (weaponRange/Math.sqrt(2)), currentWorm.position.x) && between(myWorms[i].position.y, currentWorm.position.y - (int) (weaponRange/Math.sqrt(2)), currentWorm.position.y) && isDiagonal(currentWorm, myWorms[i]))) {
                    found = true;
                }

            }
            i++;
        }
        return found;
    }

//    Mengecek apakah worm 1 dan worm 2 diagonal
    private boolean isDiagonal(Worm worm1, Worm worm2){
        return Math.abs(worm1.position.x - worm2.position.x) == Math.abs(worm1.position.y - worm2.position.y);
    }

//    Mencari cell di sekitar currentWorm yang kosong
    private Cell searchEmptySurrounding(){
        List <Cell> blocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);
        for (Cell block : blocks){
            if(!isCellOccupied(block)) return block;
        }
        return null;
    }

//    Mencari cell untuk kabur dari lava
    private Cell escapeLava(){
        List <Cell> surround = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);

        if(isSurroundedByDirtAndLava()){
            for (Cell block : surround){
                if(block.type == CellType.DIRT) return block;
            }
        } else {
            for (Cell block : surround){
                if(block.type == CellType.AIR) return block;
            }
        }

        return null;
    }


    /*
     * TODO Fungsi kelayakan, pengecekan untuk tidak melakukan invalid command.
     * bingung parameternya hehe, yang penting gunain isCellOccupied, isDirt,
     * isDeepSpace Worms gabisa gerak ke cell yg ga adjacent -> udah ada
     * getSurroundingCells Worms cannot move to cells occupied by another worm Worms
     * cannot move to dirt or deep space cells
     */
    // private boolean isMoveValid

}
// cd src/main/java/za/co/entelect/challenge