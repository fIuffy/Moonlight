package me.fluffy.dactyl.gui.impl.setting;

import me.fluffy.dactyl.Dactyl;
import me.fluffy.dactyl.setting.Setting;
import me.fluffy.dactyl.util.Bind;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;

public class BooleanElement extends SettingElement {
    public BooleanElement(Setting setting, int x, int y) {
        super(setting, x, y);
    }

    @Override
    public void drawScreen(int drawX, int drawY, float partialTicks) {
        this.drawDefaultBackground(drawX, drawY, (Boolean) this.getSetting().getValue());
        Dactyl.fontUtil.drawString(this.getSetting().getName(), this.getX() + 4, this.getY() + (15 - 8) / 2, 0xffffffff);
    }

    @Override
    public void mouseClicked(int mouseButton, int x, int y) {
        if (isHovering(x, y)) {
            boolean currentVal = (boolean) this.getSetting().getValue();
            this.getSetting().setValue(!currentVal);
        }
    }
}
