package com.grizmtech.perms.util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PHP json_encode çıktılarında boş ilişkisel dizilerin [] (JSON Array) olarak serileştirilmesi
 * durumunu sorunsuz karşılayan paylaşımlı Gson yapılandırması.
 */
public final class JsonHelper {

    public static final Type STRING_LIST_MAP_TYPE = new TypeToken<Map<String, List<String>>>() {}.getType();

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .registerTypeAdapter(STRING_LIST_MAP_TYPE, new PhpSafeMapDeserializer())
            .create();

    private JsonHelper() {
    }

    private static class PhpSafeMapDeserializer implements JsonDeserializer<Map<String, List<String>>> {
        @Override
        public Map<String, List<String>> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            Map<String, List<String>> result = new HashMap<>();
            if (json == null || json.isJsonNull() || json.isJsonArray()) {
                // PHP boş dizi [] döndürdüyse boş Map olarak kabul et
                return result;
            }
            if (json.isJsonObject()) {
                JsonObject obj = json.getAsJsonObject();
                for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                    List<String> list = new ArrayList<>();
                    JsonElement val = entry.getValue();
                    if (val != null && val.isJsonArray()) {
                        for (JsonElement item : val.getAsJsonArray()) {
                            if (item != null && item.isJsonPrimitive()) {
                                list.add(item.getAsString());
                            }
                        }
                    }
                    result.put(entry.getKey().toLowerCase(), list);
                }
            }
            return result;
        }
    }
}

