import android.view.Menu;
import net.gsantner.markor.R;

public class ExtendedView {
    public static void hideMenu(Menu menu) {
        menu.setGroupVisible(R.id.actions, false);
    }
}
