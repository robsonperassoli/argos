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
package br.com.ezequieljuliano.argos.persistence;

import br.com.ezequieljuliano.argos.domain.EventoTipo;
import java.util.List;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ezequiel Juliano Müller
 */
@Repository
public class EventoTipoDAO extends GenericDAO<EventoTipo, String> {

    private static final long serialVersionUID = 1L;

    public EventoTipo findByCodigo(int codigo) {
        Query query = new Query(Criteria.where("codigo").is(codigo));
        return getMongoOperations().findOne(query, EventoTipo.class);
    }

    public List<EventoTipo> findListByDescricao(String descricao) {
        Query query = new Query(Criteria.where("descricao").regex(descricao, "i"));
        return getMongoOperations().find(query, EventoTipo.class);
    }
}
