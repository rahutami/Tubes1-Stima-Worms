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
    }

    private MyWorm getCurrentWorm(GameState gameState) {
        return Arrays.stream(gameState.myPlayer.worms).filter(myWorm -> myWorm.id == gameState.currentWormId)
                .findFirst().get();
    }
    // Cek x1 between x2 sama x3 apa ga

    private boolean between(int x1, int x2, int x3) {
        if (x2 > x3) {
            return x1 >= x3 && x1 <= x2;
        } else {
            return x1 <= x3 && x1 >= x2;
        }
    }

    public Command run() {
        // Kalo ada enemy yang bisa ditembak -> ditembak
        Worm enemyWorm = getFirstWormInRange();
        if (enemyWorm != null && enemyWorm.health > 0) {
            Direction direction = resolveDirection(currentWorm.position, enemyWorm.position);
            return new ShootCommand(direction);
        }

        // Nyari worm enemy yang paling deket
        Worm closestEnemy = opponent.worms[1];
        double closestDistance = 50;
        for (Worm enemyWorms : opponent.worms) {
            double accumDistance = 0;
            for (Worm myWorm : gameState.myPlayer.worms) {
                accumDistance += euclideanDistance(myWorm.position.x, myWorm.position.y, enemyWorms.position.x,
                        enemyWorms.position.y);
            }

            if (accumDistance < closestDistance) {
                closestDistance = accumDistance;
                closestEnemy = enemyWorms;
            }
        }

        List<Cell> surroundingBlocks = getSurroundingCells(currentWorm.position.x, currentWorm.position.y);

        Cell chosenCell = chooseCell(surroundingBlocks, closestEnemy.position.x, closestEnemy.position.y);

        if (chosenCell.type == CellType.AIR) {
            return new MoveCommand(chosenCell.x, chosenCell.y);
        } else if (chosenCell.type == CellType.DIRT) {
            return new DigCommand(chosenCell.x, chosenCell.y);
        }

        return new DoNothingCommand();
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

    private Worm getFirstWormInRange() {

        Set<String> cells = constructFireDirectionLines(currentWorm.weapon.range).stream().flatMap(Collection::stream)
                .map(cell -> String.format("%d_%d", cell.x, cell.y)).collect(Collectors.toSet());

        for (Worm enemyWorm : opponent.worms) {
            String enemyPosition = String.format("%d_%d", enemyWorm.position.x, enemyWorm.position.y);
            if (cells.contains(enemyPosition)) {
                return enemyWorm;
            }
        }

        return null;
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
                if (i != x && j != y && isValidCoordinate(i, j)) {
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

        if (verticalComponent < 0) {
            builder.append('N');
        } else if (verticalComponent > 0) {
            builder.append('S');
        }

        if (horizontalComponent < 0) {
            builder.append('W');
        } else if (horizontalComponent > 0) {
            builder.append('E');
        }

        return Direction.valueOf(builder.toString());
    }

    private boolean isDirt(Cell c) {
        return (c.type == CellType.DIRT);
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
        return (foundOpponentWorm && foundPlayerWorm);
    }

    // Cek apakah cell surrounded by dirt
    private boolean isSurroundedByDirt(Cell c) {
        List<Cell> surrounding = getSurroundingCells(c.x, c.y);
        int i = 0;
        boolean isAllDirt = true;
        while (i < surrounding.size() && (!isAllDirt)) {
            if (!isDirt(surrounding.get(i))) {
                isAllDirt = false;
            } else {
                i++;
            }
        }
        return isAllDirt;
    }

    // Cek apakah cell surrounded by deep space
    private boolean isSurroundedByDeepSpace(Cell c) {
        List<Cell> surrounding = getSurroundingCells(c.x, c.y);
        int i = 0;
        boolean isAllDeepSpace = true;
        while (i < surrounding.size() && (!isAllDeepSpace)) {
            if (!isDeepSpace(surrounding.get(i))) {
                isAllDeepSpace = false;
            } else {
                i++;
            }
        }
        return isAllDeepSpace;
    }

    // Cari posisi worm lawan yang health nya minimum
    private Position getPositionWormMinHealth() {
        Worm[] opponentWorms = opponent.worms;
        int min = opponentWorms[0].health;
        int i;
        for (i = 0; i < opponentWorms.length; i++) {
            if (opponentWorms[i].health < min) {
                min = opponentWorms[i].health;
            }
        }
        return (opponentWorms[i].position);
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