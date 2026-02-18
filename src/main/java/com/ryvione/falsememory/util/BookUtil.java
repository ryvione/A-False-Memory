package com.ryvione.falsememory.util;

import net.minecraft.network.chat.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

public class BookUtil {

    public static ItemStack createWrittenBook(String title, String author, List<String> rawPages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        CompoundTag tag = new CompoundTag();
        tag.putString("title", title);
        tag.putString("author", author);
        ListTag pages = new ListTag();
        for (String raw : rawPages) {
            pages.add(StringTag.valueOf(pageJson(raw)));
        }
        tag.put("pages", pages);
        book.applyComponents(net.minecraft.core.component.DataComponentPatch.builder()
            .set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(tag)).build());
        return book;
    }

    public static String pageJson(String text) {
        return Component.Serializer.toJson(Component.literal(text), RegistryAccess.EMPTY);
    }

    public static ItemStack createSinglePageBook(String title, String author, String pageText) {
        return createWrittenBook(title, author, List.of(pageText));
    }
}