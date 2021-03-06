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
package br.com.ezequieljuliano.argos.domain;

import javax.inject.Named;

/**
 *
 * @author Ezequiel Juliano Müller
 */
@Named
public enum DashboardTipo {
    
    dtListagemLogs("Últimos Eventos (Logs) Recebidos"),
    dtGraficoEvolucao("Evolução Diária de Eventos (Logs)"),
    dtGraficoNiveis("Percentual de Eventos (Logs) por Níveis"),
    dtGraficoTipos("Percentual de Eventos (Logs) por Tipos"),
    dtGraficoHosts("Percentual de Eventos (Logs) por Hosts"),
    dtGraficoUsers("Percentual de Eventos (Logs) por Usuários do Sistema"),
    dtGraficoFontes("Percentual de Eventos (Logs) por Fontes");
    
    private static final long serialVersionUID = 1L;
    
    private String descricao;
    
    private DashboardTipo(String descricao) {
        this.descricao = descricao;
    }
    
    public String getDescricao() {
        return descricao;
    }
   
}
