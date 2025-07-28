import android.view.Menu;
import net.gsantner.markor.R;

public class ExtendedView {
    public static void hideMenu(Menu menu) {
        menu.setGroupVisible(R.id.document__edit__menu, false);
        menu.setGroupVisible(R.id.main__bottom__nav, false);
    }
}
