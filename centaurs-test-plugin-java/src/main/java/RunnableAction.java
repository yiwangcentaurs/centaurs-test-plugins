/**
 * Created by Feliciano on 11/21/2017.
 */
public abstract class RunnableAction {

    String actionName;

    RunnableAction(String actionName) {
        this.actionName = actionName;
    }

    abstract boolean run() throws Exception;

    public String getActionName() {
        return actionName;
    }

}
