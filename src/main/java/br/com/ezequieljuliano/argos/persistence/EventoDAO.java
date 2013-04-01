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
import br.com.ezequieljuliano.argos.domain.Usuario;
import br.com.ezequieljuliano.argos.domain.UsuarioPerfil;
import br.com.ezequieljuliano.argos.domain.UsuarioTermoPesquisaAlerta;
import br.com.ezequieljuliano.argos.util.Data;
import java.util.List;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

/**
 *
 * @author Ezequiel Juliano Müller
 */
@Repository
public class EventoDAO extends GenericLuceneDAO<Evento, String> {

    private static final long serialVersionUID = 1L;
    
    private Filter luceneFilter = null;
    
    @Autowired
    private EntidadeDAO entidadeDAO;

    public List<Evento> findByPesquisaFiltro(EventoPesquisaFiltro eventoPesquisaFiltro) {
        //Reset no filter
        luceneFilter = null;
        //Monta filtro padrão
        BooleanQuery booleanQuery = new BooleanQuery();
        //Verifica se o filtro possui entidade
        if (eventoPesquisaFiltro.getEntidade() != null) {
            booleanQuery.add(new TermQuery(new Term(Constantes.INDICE_EVENTO_ENTIDADEID, eventoPesquisaFiltro.getEntidade().getId())), BooleanClause.Occur.MUST);
        } else {
            //Caso não tenha sido selecionada uma entidade pega as entidades relacionadas ao usuário
            //Se for administrador traz todas as entidades
            if (!eventoPesquisaFiltro.getUsuario().getPerfil().equals(UsuarioPerfil.administrador)) {
                List<Entidade> entidades = entidadeDAO.findByUsuario(eventoPesquisaFiltro.getUsuario());
                for (Entidade entidade : entidades) {
                    booleanQuery.add(new TermQuery(new Term(Constantes.INDICE_EVENTO_ENTIDADEID, entidade.getId())), BooleanClause.Occur.SHOULD);
                }
            }
        }
        //Verifica se o filtro possui tipo
        if (eventoPesquisaFiltro.getEventoTipo() != null) {
            booleanQuery.add(new TermQuery(new Term(Constantes.INDICE_EVENTO_TIPOID, eventoPesquisaFiltro.getEventoTipo().getId())), BooleanClause.Occur.MUST);
        }
        //Verifica se o filtro possui nível
        if (eventoPesquisaFiltro.getEventoNivel() != null) {
            booleanQuery.add(new TermQuery(new Term(Constantes.INDICE_EVENTO_NIVELID, eventoPesquisaFiltro.getEventoNivel().getId())), BooleanClause.Occur.MUST);
        }
        //Varifica se existem filtros criados
        if (!booleanQuery.clauses().isEmpty()) {
            QueryWrapperFilter queryWrapperFilter = new QueryWrapperFilter(booleanQuery);
            luceneFilter = queryWrapperFilter;
        }
        return super.luceneParserQuery(eventoPesquisaFiltro.getTermoDoFiltro().getLuceneIndex(), eventoPesquisaFiltro.getTextoDaPesquisa());
    }

    public Boolean possuiTermosDeAlerta(Evento evento, Usuario usuario) {
        if (!usuario.getTermosAlerta().isEmpty()) {
            //Cria query principal
            BooleanQuery booleanQuery = new BooleanQuery();
            //Adiciona condição para pegar somente o evento em questão
            booleanQuery.add(new TermQuery(new Term(Constantes.INDICE_EVENTO_ID, evento.getId())), BooleanClause.Occur.MUST);
            //Cria nova query para os termos)
            BooleanQuery bqTerms = new BooleanQuery();
            for (UsuarioTermoPesquisaAlerta obj : usuario.getTermosAlerta()) {
                bqTerms.add(new TermQuery(new Term(obj.getTermo().getLuceneIndex(), obj.getValor())), BooleanClause.Occur.SHOULD);
            }
            //Adciona termos (QueryPrincipal AND QueryTermos(TERMO OU TERMO ...) 
            booleanQuery.add(bqTerms, BooleanClause.Occur.MUST);
            return (!super.luceneExecQuery(booleanQuery).isEmpty());
        }
        return false;
    }

    @Override
    public Document getLuceneDocument(Evento obj) {
        Document document = new Document();
        document.add(new StringField(Constantes.INDICE_EVENTO_ID, obj.getId(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_MENSAGEM, obj.getMensagem(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_HOSTNAME, obj.getHostName(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_HOSTUSER, obj.getHostUser(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_HOSTIP, obj.getHostIp(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_FONTE, obj.getFonte(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_NOME, obj.getNome(), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_OCORRENCIADTHR, Data.timestampToString(obj.getOcorrenciaDtHr()), Field.Store.YES));
        document.add(new TextField(Constantes.INDICE_EVENTO_PALAVRASCHAVE, obj.getPalavrasChave(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_ENTIDADEID, obj.getEntidade().getId(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_ENTIDADENOME, obj.getEntidade().getNome(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_ENTIDADECADASTRONACIONAL, obj.getEntidade().getCadastroNacional(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_NIVELID, obj.getEventoNivel().getId(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_NIVELDESCRICAO, obj.getEventoNivel().getDescricao(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_TIPOID, obj.getEventoTipo().getId(), Field.Store.YES));
        document.add(new StringField(Constantes.INDICE_EVENTO_TIPODESCRICAO, obj.getEventoTipo().getDescricao(), Field.Store.YES));
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
        return luceneFilter;
    }

    @Override
    public String getValueIdKey(Evento obj) {
        return obj.getId();
    }
}
