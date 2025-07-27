import android.view.Menu;
import android.view.MenuItem;

public class MenuUtils {
    public static void hideActionsMenu(Menu menu) {
        if (menu != null) {
            MenuItem actions = menu.findItem(R.id.actions);
            if (actions != null) {
                actions.setVisible(false);
            }
        }
    }
}
