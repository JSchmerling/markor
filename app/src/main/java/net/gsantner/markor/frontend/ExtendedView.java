import android.view.Menu;

public class MenuUtils {
    public static void hideActionsMenu(Menu menu) {
        Menu actions = menu.findItem(R.id.actions);
        actions.setVisible(false);
    }
}
