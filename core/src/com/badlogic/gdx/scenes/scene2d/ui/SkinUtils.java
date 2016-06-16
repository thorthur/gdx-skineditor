package com.badlogic.gdx.scenes.scene2d.ui;

import java.io.StringWriter;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.List.ListStyle;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane.ScrollPaneStyle;
import com.badlogic.gdx.scenes.scene2d.ui.Skin.TintedDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.JsonValue.PrettyPrintSettings;
import com.badlogic.gdx.utils.JsonWriter;
import com.badlogic.gdx.utils.JsonWriter.OutputType;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.ObjectMap.Entry;
import com.badlogic.gdx.utils.reflect.ClassReflection;
import com.badlogic.gdx.utils.reflect.Field;

public class SkinUtils {

	/** Store all resources in the specified skin JSON file. */
	public static boolean save(Skin skin, FileHandle skinFile) {

		StringWriter jsonText = new StringWriter();
		JsonWriter writer = new JsonWriter(jsonText);
		Json json = new Json();
		json.setWriter(writer);

		json.writeObjectStart();

		// Sort items
		Class<?>[] items = new Class[] {
			Color.class,
			BitmapFont.class,
			TintedDrawable.class,
			ProgressBar.ProgressBarStyle.class,
			TextButton.TextButtonStyle.class,
			ImageButton.ImageButtonStyle.class,
			SplitPane.SplitPaneStyle.class,
			Touchpad.TouchpadStyle.class,
			Button.ButtonStyle.class,
			Window.WindowStyle.class,
			TextField.TextFieldStyle.class,
			ScrollPane.ScrollPaneStyle.class,
			Label.LabelStyle.class,
			List.ListStyle.class,
			CheckBox.CheckBoxStyle.class,
			Tree.TreeStyle.class,
			Slider.SliderStyle.class,
			SelectBox.SelectBoxStyle.class
		};

		for (Class<?> item : items) {
			ObjectMap<String, Object> typeResources = skin.resources.get(item);
			
			if(item == TintedDrawable.class) {
				typeResources = skin.resources.get(Drawable.class);
				Iterator<Entry<String, Object>> it = typeResources.iterator();
				while(it.hasNext()) {
					Entry<String, Object> entry = it.next();
					if(!(entry.value instanceof BaseDrawable)) {
						it.remove();
					}
				}
			}

			String name = item.getName();
			
			json.writeObjectStart(name);

			// Build a temporary array for string keys to prevent nested
			// iterators with getObjetName function.
			for (String style : typeResources.keys().toArray()) {
				
				Object res = typeResources.get(style);

				// Handle goddamn tinted drawables
				if (res instanceof BaseDrawable) {
					try {
					BaseDrawable drawable = (BaseDrawable) res;
					String n = drawable.getName();
					Pattern p = Pattern.compile("(.+)\\s\\((.+), (.+)\\)");
					Matcher m = p.matcher(n);
					if(m.matches()) {
						json.writeObjectStart(style);
						json.writeValue("name", m.group(2));
						json.writeValue("color", Color.valueOf(m.group(3)));
						json.writeObjectEnd();
					}
					} catch(Exception e) {
						e.printStackTrace();
					}
					continue;
				}

				json.writeObjectStart(style);
				Field[] fields = ClassReflection.getFields(res.getClass());

				// Handle functions
				if (res instanceof BitmapFont) {
					BitmapFont font = (BitmapFont) res;
					json.writeValue("file", font.getData().fontFile.name());
				}

				// Handle fields

				for (Field field : fields) {

					try {

						Object object = field.get(res);
						if (object != null) {
							if (object instanceof BitmapFont) {

								String value = resolveObjectName(skin, BitmapFont.class, object);
								if (value != null) {
									json.writeValue(field.getName(), value);
								}

							} else if (object instanceof Float) {

								if ((Float) object != 0.0f) {
									json.writeValue(field.getName(), object);
								}

							} else if (object instanceof Color) {
								if (res instanceof Color) {
									// Skip sub-color
								} else {
									String value = null;
									value = resolveObjectName(skin, Color.class, object);
									if (value != null) {
										json.writeValue(field.getName(), value);
									} else {
										json.writeValue(field.getName(), object);
									}
								}
							} else if (object instanceof Drawable) {
								if (res instanceof Skin.TintedDrawable) {
									// Skip drawable if it is from tinted
									// drawable
								} else {
									String value = null;
									value = resolveObjectName(skin, Drawable.class, object);
									//
									if (value != null) {
										json.writeValue(field.getName(), value);
									}
								}
							} else if (object instanceof ListStyle) {
								String value = resolveObjectName(skin, ListStyle.class, object);
								if (value != null) {
									json.writeValue(field.getName(), value);
								}

							} else if (object instanceof ScrollPaneStyle) {
								String value = resolveObjectName(skin, ScrollPaneStyle.class, object);
								if (value != null) {
									json.writeValue(field.getName(), value);
								}

							} else if (object instanceof String) {
								// only used to get original drawable for tinted
								// drawable
								json.writeValue(field.getName(), object);

							} else if (object instanceof char[]) {
								// Don't store.
							} else {
								throw new IllegalArgumentException(
										"resource object type is unknown: " + object.getClass().getCanonicalName());
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				json.writeObjectEnd();

			}

			json.writeObjectEnd();
		}
		json.writeObjectEnd();

		PrettyPrintSettings settings = new PrettyPrintSettings();
		settings.outputType = OutputType.minimal;
		settings.singleLineColumns = 100;
		skinFile.writeString(json.prettyPrint(jsonText.toString(), settings), false);

		return true;
	}

	/**
	 * Retrieve the textual name of an object
	 */
	public static String resolveObjectName(Skin skin, Class<?> classType, Object object) {

		@SuppressWarnings("rawtypes")
		ObjectMap<Class, ObjectMap<String, Object>> resources = skin.resources;
		
		if (resources.get(classType) == null) {
			return null;
		}

		Iterator<String> keys = resources.get(classType).keys();
		while (keys.hasNext()) {

			String key = keys.next();
			Object obj = resources.get(classType).get(key);

			if (obj.equals(object)) {
				return key;
			}

		}
		return null;
	}
}
