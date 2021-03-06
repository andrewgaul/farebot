/*
 * RawCard.java
 *
 * This file is part of FareBot.
 * Learn more at: https://codebutler.github.io/farebot/
 *
 * Copyright (C) 2016 Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.card;

import android.support.annotation.NonNull;

import com.codebutler.farebot.ByteArray;
import com.codebutler.farebot.card.cepas.raw.RawCEPASCard;
import com.codebutler.farebot.card.classic.raw.RawClassicCard;
import com.codebutler.farebot.card.desfire.raw.RawDesfireCard;
import com.codebutler.farebot.card.felica.raw.RawFelicaCard;
import com.codebutler.farebot.card.ultralight.raw.RawUltralightCard;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.internal.Streams;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public interface RawCard<T extends Card> {

    @NonNull
    CardType cardType();

    @NonNull
    ByteArray tagId();

    @NonNull
    Date scannedAt();

    @NonNull
    T parse();

    class GsonTypeAdapterFactory implements TypeAdapterFactory {

        static final Map<CardType, Class<? extends RawCard>> CLASSES
                = ImmutableMap.<CardType, Class<? extends RawCard>>builder()
                .put(CardType.MifareDesfire, RawDesfireCard.class)
                .put(CardType.MifareClassic, RawClassicCard.class)
                .put(CardType.MifareUltralight, RawUltralightCard.class)
                .put(CardType.CEPAS, RawCEPASCard.class)
                .put(CardType.FeliCa, RawFelicaCard.class)
                .build();

        static final String KEY_CARD_TYPE = "cardType";

        @Override
        @SuppressWarnings("unchecked")
        public <T> TypeAdapter<T> create(final Gson gson, TypeToken<T> type) {
            if (!RawCard.class.isAssignableFrom((Class) type.getRawType())) {
                return null;
            }
            final Map<CardType, TypeAdapter<RawCard>> delegates = new HashMap<>();
            for (Map.Entry<CardType, Class<? extends RawCard>> entry : CLASSES.entrySet()) {
                TypeAdapter<RawCard> delegateAdapter
                        = (TypeAdapter<RawCard>) gson.getDelegateAdapter(this, TypeToken.get(entry.getValue()));
                delegates.put(entry.getKey(), delegateAdapter);
            }
            return (TypeAdapter<T>) new RawCardTypeAdapter(delegates);
        }

        private static class RawCardTypeAdapter extends TypeAdapter<RawCard> {

            @NonNull private final Map<CardType, TypeAdapter<RawCard>> mDelegates;

            RawCardTypeAdapter(@NonNull Map<CardType, TypeAdapter<RawCard>> delegates) {
                mDelegates = delegates;
            }

            @Override
            public void write(JsonWriter out, RawCard value) throws IOException {
                TypeAdapter<RawCard> delegateAdapter = mDelegates.get(value.cardType());
                JsonObject jsonObject = delegateAdapter.toJsonTree(value).getAsJsonObject();
                jsonObject.add(KEY_CARD_TYPE, new JsonPrimitive(value.cardType().name()));
                Streams.write(jsonObject, out);
            }

            @Override
            public RawCard read(JsonReader in) throws IOException {
                JsonElement rootElement = Streams.parse(in);
                JsonElement typeElement = rootElement.getAsJsonObject().remove(KEY_CARD_TYPE);
                CardType cardType = Enum.valueOf(CardType.class, typeElement.getAsString());
                TypeAdapter<RawCard> delegateAdapter = mDelegates.get(cardType);
                return delegateAdapter.fromJsonTree(rootElement);
            }
        }
    }
}
