import android.view.Menu;
import net.gsantner.markor.R;

public class MenuUtils {
    public static void hideMenu(Menu menu) {
        menu.setGroupVisible(R.id.actions, false);
    }
}
