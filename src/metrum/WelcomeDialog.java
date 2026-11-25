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
        boolean forced = Vars.dataDirectory.child("metrum-showwelcome").exists(); // testing aid
        if (!forced && version.equals(Core.settings.getString(key, ""))) return;
        show(version);
    }

    public static void show() {
        Mods.LoadedMod mod = Vars.mods.getMod(MetrumMod.class);
        show(mod == null ? "?" : mod.meta.version);
    }

    public static void showFeatures() {
        BaseDialog dialog = new BaseDialog(Core.bundle.get("metrum.features.title"));
        float scale = arc.scene.ui.layout.Scl.scl(1f);
        float width = Math.min(Core.graphics.getWidth() / scale - 80f, 640f);
        dialog.cont.pane(list -> {
            list.defaults().width(width).left();
            for (String section : new String[]{"industry", "mining", "drones", "physics", "campaign"}) {
                String key = "metrum.features." + section;
                list.add(Core.bundle.get(key + ".title")).color(mindustry.graphics.Pal.accent).wrap().padTop(12f).row();
                list.add(Core.bundle.get(key + ".text")).wrap().padBottom(8f).row();
            }
        }).maxHeight(Core.graphics.getHeight() * 0.72f / scale);
        dialog.addCloseButton();
        dialog.show();
    }

    private static void show(String version) {
        BaseDialog d = new BaseDialog(Core.bundle.format("metrum.welcome.title", version));
        float w = Math.min(Core.graphics.getWidth() / arc.scene.ui.layout.Scl.scl(1f) - 80f, 640f);
        d.cont.pane(p -> {
            p.defaults().width(w).left();
            // header
            p.table(mindustry.gen.Tex.button, h -> {
                h.image(mindustry.gen.Icon.box).size(40f).color(mindustry.graphics.Pal.accent).padRight(12f);
                h.add(Core.bundle.get("metrum.welcome.thanks")).wrap().growX().fontScale(1.15f);
            }).pad(4f).padBottom(14f).row();
            // changes: one row per line, with a coloured bullet and alternating background
            p.add("[accent]" + Core.bundle.get("metrum.welcome.changes")).padBottom(6f).row();
            p.image().color(mindustry.graphics.Pal.accent).height(3f).growX().padBottom(8f).row();
            arc.scene.style.Drawable stripe = ((arc.scene.style.TextureRegionDrawable) mindustry.gen.Tex.whiteui)
                .tint(new arc.graphics.Color(1f, 1f, 1f, 0.06f));
            String[] lines = Core.bundle.get("metrum.changelog").split("\n");
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i].trim();
                if (line.startsWith("-")) line = line.substring(1).trim();
                if (line.isEmpty()) continue;
                String text = line;
                int idx = i;
                p.table(idx % 2 == 0 ? stripe : null, r -> {
                    r.add("[accent]>").padRight(10f).top();
                    r.add(text).wrap().growX().left();
                }).pad(2f).row();
            }
            // support
            p.image().color(mindustry.graphics.Pal.darkerGray).height(2f).growX().padTop(14f).padBottom(8f).row();
            p.add("[lightgray]" + Core.bundle.get("metrum.welcome.support")).wrap().row();
        }).maxHeight(Core.graphics.getHeight() * 0.72f / arc.scene.ui.layout.Scl.scl(1f));
        d.buttons.defaults().size(250f, 54f).pad(4f);
        d.buttons.button("GitHub: halk6000", mindustry.gen.Icon.github, () -> Core.app.openURI("https://github.com/halk6000"));
        d.buttons.button("akinoxmusic.pl", mindustry.gen.Icon.link, () -> Core.app.openURI("https://akinoxmusic.pl"));
        d.buttons.button("@ok", mindustry.gen.Icon.ok, () -> {
            Core.settings.put(key, version);
            d.hide();
        });
        d.closeOnBack(() -> Core.settings.put(key, version));
        d.show();
    }
}
