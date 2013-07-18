/*
 * Copyright 2012 Ezequiel Juliano Müller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package br.com.ezequieljuliano.argos.statistics;

import br.com.ezequieljuliano.argos.util.Data;
import java.util.Date;
import java.util.Objects;

/**
 *
 * @author Ezequiel Juliano Müller
 */
public class EventoEvolucaoObjSTS implements Comparable<EventoEvolucaoObjSTS>{

    private final String data;
    private final Integer quantidade;

    public EventoEvolucaoObjSTS(String data, Integer quantidade) {
        this.data = data;
        this.quantidade = quantidade;
    }

    public String getData() {
        return data;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.data);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final EventoEvolucaoObjSTS other = (EventoEvolucaoObjSTS) obj;
        if (!Objects.equals(this.data, other.data)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(EventoEvolucaoObjSTS o) {
        Date thisDate = Data.stringToDate(this.data);
        Date otherDate = Data.stringToDate(o.data);
        return thisDate.compareTo(otherDate);
    }
}
