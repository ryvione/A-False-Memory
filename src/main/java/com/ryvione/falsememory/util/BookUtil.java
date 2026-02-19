package com.ryvione.falsememory.util;

import net.minecraft.network.chat.Component;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.server.network.Filterable;  

import java.util.ArrayList;
import java.util.List;

public class BookUtil {

    public static ItemStack createWrittenBook(String title, String author, List<String> rawPages) {
        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);

    List<Filterable<Component>> pages = new ArrayList<>();
    for (String raw : rawPages) {
        pages.add(Filterable.passThrough(Component.literal(raw)));
    }
    WrittenBookContent content = new WrittenBookContent(
        Filterable.passThrough(title),
        author,
        0,
        pages,
        false
);

        book.applyComponents(net.minecraft.core.component.DataComponentPatch.builder()
                .set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT, content)
                .build());

        return book;
    }

    public static String pageJson(String text) {
        return Component.Serializer.toJson(Component.literal(text), RegistryAccess.EMPTY);
    }

    public static ItemStack createSinglePageBook(String title, String author, String pageText) {
        return createWrittenBook(title, author, List.of(pageText));
    }
}