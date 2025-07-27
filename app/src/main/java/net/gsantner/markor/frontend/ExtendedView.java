import android.view.Menu;
import net.gsantner.markor.R;

public class MenuUtils {
    public static void hideMenu(Menu menu) {
        Menu actions = menu.findViewbyId(R.id.actions);

        actions.setVisibility(View.GONE);
    }
}
