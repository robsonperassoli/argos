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

import br.com.ezequieljuliano.argos.constant.Constantes;
import br.com.ezequieljuliano.argos.domain.Entidade;
import br.com.ezequieljuliano.argos.domain.Evento;
import br.com.ezequieljuliano.argos.domain.EventoPesquisaFiltro;
import br.com.ezequieljuliano.argos.domain.EventoTipo;
import br.com.ezequieljuliano.argos.domain.Usuario;
import br.com.ezequieljuliano.argos.domain.UsuarioPerfil;
import br.com.ezequieljuliano.argos.domain.UsuarioTermoPesquisa;
import br.com.ezequieljuliano.argos.statistics.EventoEvolucaoObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoHostObjSTS;
import br.com.ezequieljuliano.argos.statistics.EventoTipoObjSTS;
import br.com.ezequieljuliano.argos.util.Data;
import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.CommandResult;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.DBRef;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ezequiel Juliano Müller
 */
@Repository
public class EventoDAO extends GenericLuceneDAO<Evento, String> {

    private static final long serialVersionUID = 1L;
    private int numHitsResults = 100;
    private Filter luceneFilter = null;
    
    @Autowired
    private EntidadeDAO entidadeDAO;
    
    @Autowired
    private EventoTipoDAO eventoTipoDAO;

    public List<Evento> findUltimosEventos(Usuario usuario, Integer limit) {
        List<Entidade> entidades = entidadeDAO.findByUsuario(usuario);
        Query query = new Query(Criteria.where("entidade").in(entidades));
        query.with(new Sort(Sort.Direction.DESC, "ocorrenciaDtHr"));
        query.limit(limit);
        return getMongoOperations().find(query, Evento.class);
    }

    public List<Evento> findByUsuarioAndPeriodo(Usuario usuario, Date inicio, Date fim) {
        Criteria criteriaPeriodo = new Criteria().andOperator(
                Criteria.where("ocorrenciaDtHr").gte(inicio),
                Criteria.where("ocorrenciaDtHr").lte(fim));
        List<Entidade> entidades = entidadeDAO.findByUsuario(usuario);
        Query query = new Query(Criteria.where("entidade").in(entidades).andOperator(criteriaPeriodo));
        query.with(new Sort(Sort.Direction.DESC, "ocorrenciaDtHr"));
        return getMongoOperations().find(query, Evento.class);
    }

    public List<Evento> findByPesquisaFiltro(EventoPesquisaFiltro eventoPesquisaFiltro) {
        //Limpa o filtro padrão de restrição caso houver
        this.luceneFilter = null;
        //Seta o número de Hits (Resultados da pesquisa)
        setNumHitsResults(eventoPesquisaFiltro.getNumHitsResults());
        //Filtro por entidade relacionada ao usuário
        BooleanQuery bqFilter = new BooleanQuery();
        //Se for administrador traz todas as entidades
        //Caso contrário adiciona filtro de restrição
        if (!eventoPesquisaFiltro.getUsuario().getPerfil().equals(UsuarioPerfil.administrador)) {
            List<Entidade> entidades = entidadeDAO.findByUsuario(eventoPesquisaFiltro.getUsuario());
            for (Entidade entidade : entidades) {
                bqFilter.add(new TermQuery(new Term(Constantes.INDICE_EVENTO_ENTIDADEID, entidade.getId())), BooleanClause.Occur.SHOULD);
            }
        }
        //Varifica se existem filtros de restrição adicionados
        if (!bqFilter.clauses().isEmpty()) {
            QueryWrapperFilter queryWrapperFilter = new QueryWrapperFilter(bqFilter);
            this.luceneFilter = queryWrapperFilter;
        }
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(eventoPesquisaFiltro.getBooleanQuery(), BooleanClause.Occur.MUST);
        return luceneExecQuery(booleanQuery);
    }

    public Boolean possuiTermosDeNotificacao(Evento evento, Usuario usuario) {
        if (!usuario.getTermosNotificacao().isEmpty()) {
            //Cria query principal
            BooleanQuery booleanQuery = new BooleanQuery();
            //Adiciona condição para pegar somente o evento em questão
            booleanQuery.add(new TermQuery(new Term(Constantes.INDICE_EVENTO_ID, evento.getId())), BooleanClause.Occur.MUST);
            //Cria nova query para os termos)
            BooleanQuery bqTerms = new BooleanQuery();
            for (UsuarioTermoPesquisa obj : usuario.getTermosNotificacao()) {
                try {
                    bqTerms.add(new QueryParser(Constantes.getLuceneVersion(), obj.getTermo().getLuceneIndex(), getAnalyzer()).parse(obj.getValor()), BooleanClause.Occur.SHOULD);
                } catch (ParseException ex) {
                    Logger.getLogger(EventoDAO.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //Adciona termos (QueryPrincipal AND QueryTermos(TERMO OU TERMO ...) 
            booleanQuery.add(bqTerms, BooleanClause.Occur.MUST);
            return (!luceneExecQuery(booleanQuery).isEmpty());
        }
        return false;
    }

    @Override
    public Document getLuceneDocument(Evento obj) {
        Document document = new Document();
        document.add(new StringField(Constantes.INDICE_EVENTO_ID, obj.getId(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_MENSAGEM, obj.getMensagem(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_HOSTNAME, obj.getHostName(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_HOSTUSER, obj.getHostUser(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_HOSTMAC, obj.getHostMac(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_SYSUSER, obj.getSysUser(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_HOSTIP, obj.getHostIp(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_FONTE, obj.getFonte(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_NOME, obj.getNome(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_OCORRENCIADTHR, Data.timestampToString(obj.getOcorrenciaDtHr()), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_PALAVRASCHAVE, obj.getPalavrasChave(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_ENTIDADEID, obj.getEntidade().getId(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_ENTIDADECODIGO, (String.valueOf(obj.getEntidade().getCodigo())), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_ENTIDADENOME, obj.getEntidade().getNome(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_ENTIDADECADASTRONACIONAL, obj.getEntidade().getCadastroNacional(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_NIVELID, obj.getEventoNivel().getId(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_NIVELCODIGO, String.valueOf(obj.getEventoNivel().getCodigo()), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_NIVELDESCRICAO, obj.getEventoNivel().getDescricao(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_TIPOID, obj.getEventoTipo().getId(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_TIPOCODIGO, String.valueOf(obj.getEventoTipo().getCodigo()), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_TIPODESCRICAO, obj.getEventoTipo().getDescricao(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_TUDO, getLuceneContentString(obj), Field.Store.YES));
        return document;
    }

    @Override
    public String getLuceneIndexKey() {
        return Constantes.INDICE_EVENTO_ID;
    }

    @Override
    public String getLuceneContentString(Evento obj) {
        String newLineCharacter = System.getProperty("line.separator");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(obj.getId()).append(newLineCharacter);
        stringBuilder.append(obj.getHostName()).append(newLineCharacter);
        stringBuilder.append(obj.getHostIp()).append(newLineCharacter);
        stringBuilder.append(obj.getHostUser()).append(newLineCharacter);
        stringBuilder.append(obj.getHostMac()).append(newLineCharacter);
        stringBuilder.append(obj.getSysUser()).append(newLineCharacter);
        stringBuilder.append(obj.getMensagem()).append(newLineCharacter);
        stringBuilder.append(obj.getFonte()).append(newLineCharacter);
        stringBuilder.append(obj.getNome()).append(newLineCharacter);
        stringBuilder.append(Data.timestampToString(obj.getOcorrenciaDtHr())).append(newLineCharacter);
        stringBuilder.append(obj.getPalavrasChave()).append(newLineCharacter);
        stringBuilder.append(obj.getEntidade().getId()).append(newLineCharacter);
        stringBuilder.append(obj.getEntidade().getCadastroNacional()).append(newLineCharacter);
        stringBuilder.append(obj.getEntidade().getCodigo()).append(newLineCharacter);
        stringBuilder.append(obj.getEntidade().getNome()).append(newLineCharacter);
        stringBuilder.append(obj.getEventoNivel().getId()).append(newLineCharacter);
        stringBuilder.append(obj.getEventoNivel().getCodigo()).append(newLineCharacter);
        stringBuilder.append(obj.getEventoNivel().getDescricao()).append(newLineCharacter);
        stringBuilder.append(obj.getEventoTipo().getId()).append(newLineCharacter);
        stringBuilder.append(obj.getEventoTipo().getCodigo()).append(newLineCharacter);
        stringBuilder.append(obj.getEventoTipo().getDescricao()).append(newLineCharacter);
        return stringBuilder.toString();
    }

    @Override
    public Filter getLuceneFilterRestriction() {
        return this.luceneFilter;
    }

    @Override
    public String getLuceneValueIdKey(Evento obj) {
        return obj.getId();
    }

    @Override
    public int getLuceneNumHits() {
        return getNumHitsResults();
    }

    public int getNumHitsResults() {
        return numHitsResults;
    }

    public void setNumHitsResults(int numHitsResults) {
        this.numHitsResults = numHitsResults;
    }

    public List<EventoHostObjSTS> findHostsSts(Usuario usuario, Date dataIni, Date dataFim) {
        BasicDBList idsEntidades = new BasicDBList();
        idsEntidades.addAll(getIdsFromEntidadesArray(entidadeDAO.findByUsuario(usuario)));
        
        MongoOperations mo = getMongoOperations();
        DBCollection colEvento = mo.getCollection(mo.getCollectionName(Evento.class));
                
        BasicDBList and = new BasicDBList();
        and.add(new BasicDBObject("entidade.$id", new BasicDBObject("$in", idsEntidades)));
        and.add(new BasicDBObject("ocorrenciaDtHr", new BasicDBObject("$gte", dataIni)));
        and.add(new BasicDBObject("ocorrenciaDtHr", new BasicDBObject("$lte", dataFim)));
        
        DBObject match = new BasicDBObject("$match", new BasicDBObject("$and", and));
        DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", "$hostIp").append("quantidade", new BasicDBObject("$sum", 1)));
        DBObject sort = new BasicDBObject("$sort", new BasicDBObject("quantidade", -1));
        DBObject limit = new BasicDBObject("$limit", 10);
        
        Iterable<DBObject> results = colEvento.aggregate(match, group, sort, limit).results();
        
        List<EventoHostObjSTS> resultList = new ArrayList<EventoHostObjSTS>();
        for (DBObject dBObject : results) {
            Integer qtd = (Integer) dBObject.get("quantidade");
            String host = (String) dBObject.get("_id");
            resultList.add(new EventoHostObjSTS(host, qtd));
        }
        return resultList;
    }

    private List<ObjectId> getIdsFromEntidadesArray(List<Entidade> entidades) {
        List<ObjectId> ids = new ArrayList<ObjectId>();
        for (Entidade entidade : entidades) {
            ids.add(new ObjectId(entidade.getId()));
        }
        return ids;
    }

    public List<EventoTipoObjSTS> findTiposSts(Usuario usuario, Date dataIni, Date dataFim) {
        BasicDBList idsEntidades = new BasicDBList();
        idsEntidades.addAll(getIdsFromEntidadesArray(entidadeDAO.findByUsuario(usuario)));
        
        MongoOperations mo = getMongoOperations();
        DBCollection colEvento = mo.getCollection(mo.getCollectionName(Evento.class));
                
        BasicDBList and = new BasicDBList();
        and.add(new BasicDBObject("entidade.$id", new BasicDBObject("$in", idsEntidades)));
        and.add(new BasicDBObject("ocorrenciaDtHr", new BasicDBObject("$gte", dataIni)));
        and.add(new BasicDBObject("ocorrenciaDtHr", new BasicDBObject("$lte", dataFim)));
        
        DBObject match = new BasicDBObject("$match", new BasicDBObject("$and", and));
        DBObject group = new BasicDBObject("$group", new BasicDBObject("_id", "$eventoTipo").append("quantidade", new BasicDBObject("$sum", 1)));
        
        Iterable<DBObject> results = colEvento.aggregate(match, group).results();
        
        List<EventoTipoObjSTS> resultList = new ArrayList<EventoTipoObjSTS>();
        for (DBObject dBObject : results) {
            Integer qtd = (Integer) dBObject.get("quantidade");
            DBRef eventoTipoId = (DBRef) dBObject.get("_id");
            ObjectId id = (ObjectId) eventoTipoId.getId();
            EventoTipo tipo = eventoTipoDAO.load(id.toString());
            resultList.add(new EventoTipoObjSTS(tipo.getDescricao(), qtd));
        }
        return resultList;
    }

    public List<EventoEvolucaoObjSTS> findEvolucaoSts(Usuario usuario, Date dataIni, Date dataFim) {
        BasicDBList idsEntidades = new BasicDBList();
        idsEntidades.addAll(getIdsFromEntidadesArray(entidadeDAO.findByUsuario(usuario)));
        
        MongoOperations mo = getMongoOperations();
        DBCollection colEvento = mo.getCollection(mo.getCollectionName(Evento.class));
        
        BasicDBList and = new BasicDBList();
        and.add(new BasicDBObject("entidade.$id", new BasicDBObject("$in", idsEntidades)));
        and.add(new BasicDBObject("ocorrenciaDtHr", new BasicDBObject("$gte", dataIni)));
        and.add(new BasicDBObject("ocorrenciaDtHr", new BasicDBObject("$lte", dataFim)));
        
        DBObject match = new BasicDBObject("$match", new BasicDBObject("$and", and));
        DBObject project = new BasicDBObject("$project", new BasicDBObject("dia", new BasicDBObject("$dayOfMonth", "$ocorrenciaDtHr"))
                    .append("mes", new BasicDBObject("$month", "$ocorrenciaDtHr"))
                    .append("ano", new BasicDBObject("$year", "$ocorrenciaDtHr"))
                );
        DBObject group = new BasicDBObject("$group", 
                new BasicDBObject("_id", 
                    new BasicDBObject("dia", "$dia").append("mes", "$mes").append("ano", "$ano")
                ).append("quantidade", new BasicDBObject("$sum", 1)));
        
        Iterable<DBObject> results = colEvento.aggregate(match, project, group).results();
        
        List<EventoEvolucaoObjSTS> resultList = new ArrayList<EventoEvolucaoObjSTS>();
        for (DBObject dBObject : results) {
            Integer qtd = (Integer) dBObject.get("quantidade");
            BasicDBObject id = (BasicDBObject) dBObject.get("_id");
            String data = id.get("dia") + "/" + id.get("mes") + "/" + id.get("ano");
            resultList.add(new EventoEvolucaoObjSTS(data, qtd));
        }
        Collections.sort(resultList);        
        return resultList;
    }
}
