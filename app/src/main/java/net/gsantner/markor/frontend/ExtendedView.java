import android.view.Menu;
import net.gsantner.markor.R;

public class MenuUtils {
    public static void hideActionsMenu(Menu menu) {
        Menu actions = menu.findItem(R.id.actions);
        actions.setVisible(false);
    }
}
