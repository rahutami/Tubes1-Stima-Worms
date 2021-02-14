package za.co.entelect.challenge.command;

public class SelectCommand implements Command{
    private Command nextCommand;
    private int worm;

    public SelectCommand(int worm, Command nextCommand) {
        this.worm = worm;
        this.nextCommand = nextCommand;
    }

    @Override
    public String render() {
        return String.format("select %d;", worm, nextCommand.render());
    }
}
