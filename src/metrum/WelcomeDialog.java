package metrum;

import arc.Core;
import mindustry.Vars;
import mindustry.mod.Mods;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Shown once after install or after the mod version changes.
 * Texts come from the bundles, so the player sees them in the game's language (EN default, PL).
 */
public class WelcomeDialog {
    static final String key = "metrum-seen-version";

    public static void showIfNew() {
        if (Vars.headless) return;
        Mods.LoadedMod mod = Vars.mods.getMod(MetrumMod.class);
        String version = mod == null ? "?" : mod.meta.version;
        if (version.equals(Core.settings.getString(key, ""))) return;

        BaseDialog d = new BaseDialog(Core.bundle.format("metrum.welcome.title", version));
        d.cont.pane(p -> {
            p.add(Core.bundle.get("metrum.welcome.thanks")).wrap().width(560f).left().padBottom(12f).row();
            p.add("[accent]" + Core.bundle.get("metrum.welcome.changes")).left().row();
            p.add(Core.bundle.get("metrum.changelog")).wrap().width(560f).left().padBottom(12f).row();
            p.add(Core.bundle.get("metrum.welcome.support")).wrap().width(560f).left().row();
        }).maxHeight(Core.graphics.getHeight() * 0.7f);
        d.buttons.defaults().size(210f, 54f).pad(4f);
        d.buttons.button("GitHub: halk6000", () -> Core.app.openURI("https://github.com/halk6000"));
        d.buttons.button("akinoxmusic.pl", () -> Core.app.openURI("https://akinoxmusic.pl"));
        d.buttons.button("@ok", () -> {
            Core.settings.put(key, version);
            d.hide();
        });
        d.closeOnBack(() -> Core.settings.put(key, version));
        d.show();
    }
}
