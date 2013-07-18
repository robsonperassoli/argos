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

import br.com.ezequieljuliano.argos.business.EventoBC;
import br.com.ezequieljuliano.argos.domain.DashboardTipo;
import br.com.ezequieljuliano.argos.domain.Evento;
import br.com.ezequieljuliano.argos.security.SessionAttributes;
import br.com.ezequieljuliano.argos.statistics.EventoEvolucaoObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoFonteObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoHostObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoNivelObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoSTS;
import br.com.ezequieljuliano.argos.statistics.EventoSysUserObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoTipoObjSTS;
import br.com.ezequieljuliano.argos.util.Data;
import br.gov.frameworkdemoiselle.message.MessageContext;
import br.gov.frameworkdemoiselle.message.SeverityType;
import br.gov.frameworkdemoiselle.stereotype.ViewController;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import org.primefaces.model.chart.CartesianChartModel;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartSeries;
import org.primefaces.model.chart.PieChartModel;

/**
 *
 * @author Ezequiel Juliano Müller
 */
@ViewController
public class DashboardMB {

    @Inject
    private EventoBC eventoBC;
    
    @Inject
    private SessionAttributes sessionAttributes;
    
    @Inject
    private MessageContext messageContext;
    
    private EventoSTS eventoSTS;
    private PieChartModel pieNiveis;
    private PieChartModel pieTipos;
    private CartesianChartModel linearEvolucao;
    private PieChartModel pieHosts;
    private PieChartModel pieFontes;
    private PieChartModel pieSysUsers;
    private DashboardTipo dashboardTipo = DashboardTipo.dtGraficoEvolucao;
    private ArrayList<SelectItem> listDashboardTipos;
    private Date dataIni = Data.decreaseDaysFrom(new Date(), 5);
    private Date dataFim = new Date();
    

    @PostConstruct
    public void aposConstruir() {
        startDashboard();
    }

    private void startDashboard() {
        eventoSTS = new EventoSTS(new ArrayList<Evento>());
        createPieNiveis();
        createPieTipos();
        createPieHosts();
        createPieFontes();
        createPieSysUsers();
        createLinearEvolucao();
    }

    public Boolean isEmptyEventos(){
        return eventoSTS.getEventos().isEmpty();
    }
    
    public Date getDataIni() {
        return dataIni;
    }

    public void setDataIni(Date dataIni) {
        this.dataIni = dataIni;
    }

    public Date getDataFim() {
        return dataFim;
    }

    public void setDataFim(Date dataFim) {
        this.dataFim = dataFim;
    }
    
    public PieChartModel getPieNiveis() {
        return pieNiveis;
    }

    public PieChartModel getPieTipos() {
        return pieTipos;
    }

    public CartesianChartModel getLinearEvolucao() {
        return linearEvolucao;
    }

    public PieChartModel getPieHosts() {
        return pieHosts;
    }

    public PieChartModel getPieFontes() {
        return pieFontes;
    }

    public PieChartModel getPieSysUsers() {
        return pieSysUsers;
    }

    public List<EventoObjSTS> getListagemEventos() {
        List<EventoObjSTS> listProcessEventos = eventoSTS.getProcessEventos();
        List<EventoObjSTS> list = new ArrayList<EventoObjSTS>();

        for (int i = listProcessEventos.size() - 1; i >= 0; i--) {
            list.add(listProcessEventos.get(i));
        }

        return list;
    }

    public DashboardTipo getDashboardTipo() {
        return dashboardTipo;
    }

    public void setDashboardTipo(DashboardTipo dashboardTipo) {
        this.dashboardTipo = dashboardTipo;
    }

    public List<SelectItem> getListDashboardTipos() {
        if (this.listDashboardTipos == null) {
            this.listDashboardTipos = new ArrayList<SelectItem>();
            for (DashboardTipo tipo : DashboardTipo.values()) {
                this.listDashboardTipos.add(new SelectItem(tipo, tipo.getDescricao()));
            }
        }
        return listDashboardTipos;
    }

    public void definirDashboard() {
        if ((dashboardTipo == null) || (dataIni == null) || (dataFim == null)) {
            messageContext.add("Selecione um tipo e informe as datas inicial e final!", SeverityType.ERROR);
        } else {
           startDashboard();
        }
    }

    public Boolean dashboardTipoIsListagemLogs() {
        return dashboardTipo == DashboardTipo.dtListagemLogs;
    }

    public Boolean dashboardTipoIsGraficoEvolucao() {
        return dashboardTipo == DashboardTipo.dtGraficoEvolucao;
    }

    public Boolean dashboardTipoIsGraficoFontes() {
        return dashboardTipo == DashboardTipo.dtGraficoFontes;
    }

    public Boolean dashboardTipoIsGraficoHosts() {
        return dashboardTipo == DashboardTipo.dtGraficoHosts;
    }

    public Boolean dashboardTipoIsGraficoNiveis() {
        return dashboardTipo == DashboardTipo.dtGraficoNiveis;
    }

    public Boolean dashboardTipoIsGraficoTipos() {
        return dashboardTipo == DashboardTipo.dtGraficoTipos;
    }

    public Boolean dashboardTipoIsGraficoUsers() {
        return dashboardTipo == DashboardTipo.dtGraficoUsers;
    }

    public Integer getMaxY() {
        return eventoSTS.getEventos().size();
    }

    private void createPieNiveis() {
        pieNiveis = new PieChartModel();
        List<EventoNivelObjSTS> list = eventoSTS.getProcessNiveis();
        for (EventoNivelObjSTS obj : list) {
            pieNiveis.set(obj.getDescricao(), obj.getQuantidade());
        }
    }

    private void createPieTipos() {
        pieTipos = new PieChartModel();
        List<EventoTipoObjSTS> list = eventoBC.findTiposSts(sessionAttributes.getUsuario(), dataIni, dataFim);
        for (EventoTipoObjSTS obj : list) {
            pieTipos.set(obj.getDescricao(), obj.getQuantidade());
        }
    }

    private void createPieHosts() {
        pieHosts = new PieChartModel();
        List<EventoHostObjSTS> list = eventoBC.findHostsSts(sessionAttributes.getUsuario(), dataIni, dataFim);
        for (EventoHostObjSTS obj : list) {
            pieHosts.set(obj.getHost(), obj.getQuantidade());
        }
    }

    private void createPieFontes() {
        pieFontes = new PieChartModel();
        List<EventoFonteObjSTS> list = eventoSTS.getProcessFontes();
        for (EventoFonteObjSTS obj : list) {
            pieFontes.set(obj.getDescricao(), obj.getQuantidade());
        }
    }

    private void createPieSysUsers() {
        pieSysUsers = new PieChartModel();
        List<EventoSysUserObjSTS> list = eventoSTS.getProcessSysUsers();
        for (EventoSysUserObjSTS obj : list) {
            pieSysUsers.set(obj.getDescricao(), obj.getQuantidade());
        }
    }

    private void createLinearEvolucao() {
        linearEvolucao = new CartesianChartModel();
        ChartSeries serie = new LineChartSeries(); 
        serie.setLabel("Logs");
        List<EventoEvolucaoObjSTS> list = eventoBC.findEvolucaoSts(sessionAttributes.getUsuario(), dataIni, dataFim);
        for (int i = list.size() - 1; i >= 0; i--) {
            serie.set(list.get(i).getData(), list.get(i).getQuantidade());
        }
        linearEvolucao.addSeries(serie);
    }
}
