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
package br.com.ezequieljuliano.argos.view;

import br.com.ezequieljuliano.argos.domain.Evento;
import br.com.ezequieljuliano.argos.domain.EventoTipoPesquisa;
import br.com.ezequieljuliano.argos.persistence.EventoDAO;
import br.com.ezequieljuliano.argos.persistence.EventoLuceneDAO;
import br.gov.frameworkdemoiselle.stereotype.ViewController;
import java.util.ArrayList;
import java.util.List;
import javax.faces.model.SelectItem;
import javax.inject.Inject;

/**
 *
 * @author Ezequiel Juliano Müller
 */
@ViewController
public class EventoPesquisaMB {

    private static final long serialVersionUID = 1L;
    @Inject
    private EventoLuceneDAO luceneDAO;
    @Inject
    private EventoDAO eventoDAO;
    private String campoPesquisa;
    private EventoTipoPesquisa tipoPesquisa;
    private List<Evento> eventos;

    public EventoPesquisaMB() {
        tipoPesquisa = EventoTipoPesquisa.tudo;
    }

    public String getCampoPesquisa() {
        return campoPesquisa;
    }

    public void setCampoPesquisa(String campoPesquisa) {
        this.campoPesquisa = campoPesquisa;
    }

    public EventoTipoPesquisa getTipoPesquisa() {
        return tipoPesquisa;
    }

    public void setTipoPesquisa(EventoTipoPesquisa tipoPesquisa) {
        this.tipoPesquisa = tipoPesquisa;
    }

    public List<Evento> getEventos() {
        return eventos;
    }

    public void setEventos(List<Evento> eventos) {
        this.eventos = eventos;
    }

    public void pesquisar() {
        if (campoPesquisa == null || campoPesquisa.length() == 0) {
            eventos = eventoDAO.findAll();
        } else {
            switch (tipoPesquisa) {
                case tudo:
                    eventos = luceneDAO.findByTudo(campoPesquisa);
                    break;
            }
        }
    }

    public List<SelectItem> getTipoPesquisas() {
        List<SelectItem> itens = new ArrayList<SelectItem>();

        for (EventoTipoPesquisa eventotipoPesquisa : EventoTipoPesquisa.values()) {
            itens.add(new SelectItem(eventotipoPesquisa, eventotipoPesquisa.getNome()));
        }
        return itens;
    }
}