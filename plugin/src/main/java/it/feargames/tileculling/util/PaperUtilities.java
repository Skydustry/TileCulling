package it.feargames.tileculling.util;

import org.bukkit.World;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public final class PaperUtilities {

	private static Method GET_NO_TICK_VIEW_DISTANCE_METHOD;

	static {
		try {
			try {
				// 1.18+
				GET_NO_TICK_VIEW_DISTANCE_METHOD = World.class.getDeclaredMethod("getViewDistance");
			} catch (NoSuchMethodException ignored) {
				// Paper 1.16.5 - 1.17
				GET_NO_TICK_VIEW_DISTANCE_METHOD = World.class.getDeclaredMethod("getNoTickViewDistance");
			}
		} catch (NoSuchMethodException ignored) {
			// Spigot 1.16.5 - 1.17 (fallback to view distance)
		}
	}

	private PaperUtilities() {
	}

	public static int getViewDistance(World world) {
		if (GET_NO_TICK_VIEW_DISTANCE_METHOD != null) {
			try {
				return (int) GET_NO_TICK_VIEW_DISTANCE_METHOD.invoke(world);
			} catch (IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		return world.getViewDistance();
	}

}
