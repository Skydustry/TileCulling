package it.feargames.tileculling;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Stream;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.BlockState;
import org.bukkit.configuration.ConfigurationSection;

public class HiddenTileRegistry {

    private final Logger logger;

    private Material[] hiddenMaterials;
    private String[] hiddenNamespaces;

    public HiddenTileRegistry(Logger logger) {
        this.logger = logger;
    }

    public void load(ConfigurationSection config) {
        Set<Material> materials = new HashSet<>();
        Set<String> keys = new HashSet<>();

        for (String materialName : config.getStringList("materials")) {
            Material material = Material.getMaterial(materialName);

            if (material == null || !material.isBlock()) {
                logger.warning("Material " + materialName + " is invalid!");
                continue;
            }

            materials.add(material);
        }

        for (String tagName : config.getStringList("tags")) {
            Tag<?> tag;

            try {
                tag = (Tag<?>) Tag.class.getDeclaredField(tagName).get(null);
            } catch (NoSuchFieldException e) {
                logger.warning("Material tag " + tagName + " is invalid!");
                continue;
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            if (!tag.getClass().getSimpleName().equals("CraftBlockTag")) {
                logger.warning("Material tag " + tagName + " is not a block tag!");
                continue;
            }

            Tag<Material> blockTag = (Tag<Material>) tag;
            materials.addAll(blockTag.getValues());
        }

        for (String key : config.getStringList("keys")) {
            keys.add(key);
        }

        load(materials, keys);
    }

    public void load(Collection<Material> materials, Collection<String> keys) {
        hiddenMaterials = materials.toArray(Material[]::new);
        hiddenNamespaces = Stream.concat(
                materials.stream().map(material -> material.getKey().getKey()),
                keys.stream()
        ).toArray(String[]::new);

        logger.info(() -> "Loaded " + materials.size() + " hidden tile types and " + keys.size() + " keys");
    }

    public boolean shouldHide(String namespacedKey) {
        for (String current : hiddenNamespaces) {
            if (current.equals(namespacedKey)) {
                return true;
            }
        }

        return false;
    }

    public boolean shouldHide(Material material) {
        for (Material current : hiddenMaterials) {
            if (current == material) {
                return true;
            }
        }

        return false;
    }

    public boolean shouldHide(BlockState state) {
        return shouldHide(state.getType());
    }
}
