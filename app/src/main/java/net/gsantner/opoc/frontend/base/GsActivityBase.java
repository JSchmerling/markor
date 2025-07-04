/*#######################################################
 *
 * SPDX-FileCopyrightText: 2020-2025 Gregor Santner <gsantner AT mailbox DOT org>
 * SPDX-License-Identifier: Unlicense OR CC0-1.0
 *
 * Written 2020-2025 by Gregor Santner <gsantner AT mailbox DOT org>
 * To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
 * You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.
#########################################################*/
package net.gsantner.opoc.frontend.base;

import static android.view.WindowManager.LayoutParams.FLAG_SECURE;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import net.gsantner.opoc.model.GsSharedPreferencesPropertyBackend;
import net.gsantner.opoc.util.GsContextUtils;
import net.gsantner.opoc.wrapper.GsCallback;

import java.util.concurrent.atomic.AtomicBoolean;

public abstract class GsActivityBase<AS extends GsSharedPreferencesPropertyBackend, CU extends GsContextUtils> extends AppCompatActivity {

    protected final AtomicBoolean _activityFirstTimeVisible = new AtomicBoolean(true);
    protected AS _appSettings;
    protected Bundle _savedInstanceState;
    protected GsContextUtils _cu;
    private int m_initialToolbarHeight = 0;

    private final GsCallback.a0 m_setActivityBackgroundColor = () -> GsContextUtils.instance.setActivityBackgroundColor(GsActivityBase.this, getNewActivityBackgroundColor());
    private final GsCallback.a0 m_setActivityNavigationBarColor = () -> GsContextUtils.instance.setActivityNavigationBarBackgroundColor(GsActivityBase.this, getNewNavigationBarColor());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        _savedInstanceState = savedInstanceState;
        _appSettings = createAppSettingsInstance();
        _cu = createContextUtilsInstance();

        m_setActivityBackgroundColor.callback();
        m_setActivityNavigationBarColor.callback();

        // Set secure flag / disallow screenshots
        if (isFlagSecure() != null) {
            try {
                if (isFlagSecure()) {
                    getWindow().setFlags(FLAG_SECURE, FLAG_SECURE);
                } else {
                    getWindow().clearFlags(FLAG_SECURE);
                }
            } catch (Exception ignored) {
            }
        }

        // Setup the call to toolbar click events
        // Do this every time the activity is created, to accommodate lifecycle changes
        final Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setOnClickListener(this::onToolbarClick);
            toolbar.setOnLongClickListener(this::onToolbarLongClick);
        }
    }

    protected abstract AS createAppSettingsInstance();

    protected abstract CU createContextUtilsInstance();

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        _savedInstanceState = savedInstanceState;
    }

    @Override
    protected void onResume() {
        super.onResume();
        m_setActivityBackgroundColor.callback();
        m_setActivityNavigationBarColor.callback();
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        // Only do this when not being restored
        if (_activityFirstTimeVisible.getAndSet(false) && _savedInstanceState == null) {
            new Handler(getMainLooper()).post(this::onActivityFirstTimeVisible);
        }
    }

    /**
     * This will be called when this activity gets the first time visible
     */
    public void onActivityFirstTimeVisible() {
    }

    private void onToolbarClick(final View v) {
        final FragmentManager fm = getSupportFragmentManager();
        for (final Fragment fragment : fm.getFragments()) {
            if (fragment instanceof GsFragmentBase && fragment.isVisible() && fragment.isResumed()) {
                ((GsFragmentBase<?, ?>) fragment).onToolbarClicked(v);
                break;
            }
        }
    }

    private boolean onToolbarLongClick(final View v) {
        final FragmentManager fm = getSupportFragmentManager();
        for (final Fragment fragment : fm.getFragments()) {
            if (fragment instanceof GsFragmentBase && fragment.isVisible() && fragment.isResumed()) {
                return ((GsFragmentBase<?, ?>) fragment).onToolbarLongClicked(v);
            }
        }
        return false;
    }

    @ColorInt
    public Integer getNewNavigationBarColor() {
        return null;
    }

    @ColorInt
    public Integer getNewActivityBackgroundColor() {
        return null;
    }

    public Boolean isFlagSecure() {
        return null;
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        try {
            final Toolbar t = findViewById(GsContextUtils.instance.getResId(this, GsContextUtils.ResType.ID, "toolbar"));
            if (t != null) {
                t.setTitle(title);
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Set the Activity {@link Toolbar}, or more specific it's parent AppBarLayout to visible/gone
     *
     * @param visible Show toolbar or not
     */
    public void setToolbarVisible(boolean visible) {
        try {
            LinearLayout appBarLayout = ((LinearLayout) getToolbar().getParent());
            if (!visible && m_initialToolbarHeight == 0) {
                m_initialToolbarHeight = appBarLayout.getMeasuredHeight();
            }
            ViewGroup.LayoutParams lp = appBarLayout.getLayoutParams();
            lp.height = visible ? m_initialToolbarHeight : 0;
            appBarLayout.setLayoutParams(lp);
        } catch (Exception ignored) {
        }
    }

    private Toolbar getToolbar() {
        try {
            final int toolbarResId = GsContextUtils.instance.getResId(this, GsContextUtils.ResType.ID, "toolbar");
            return findViewById(toolbarResId);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void setToolbarText(final CharSequence text) {
        final Toolbar toolbar = getToolbar();
        if (toolbar != null) {
            toolbar.setTitle(text);
        }
    }

    public AS getAppSettings() {
        return _appSettings;
    }
}
