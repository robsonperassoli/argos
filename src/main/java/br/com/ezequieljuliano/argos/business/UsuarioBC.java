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
package br.com.ezequieljuliano.argos.business;

import br.com.ezequieljuliano.argos.domain.Situacao;
import br.com.ezequieljuliano.argos.domain.Usuario;
import br.com.ezequieljuliano.argos.domain.UsuarioPerfil;
import br.com.ezequieljuliano.argos.exception.ValidationException;
import br.com.ezequieljuliano.argos.persistence.UsuarioDAO;
import br.com.ezequieljuliano.argos.util.UniqId;
import br.com.ezequieljuliano.argos.util.Utils;
import br.gov.frameworkdemoiselle.lifecycle.Startup;
import br.gov.frameworkdemoiselle.stereotype.BusinessController;
import br.gov.frameworkdemoiselle.template.DelegateCrud;
import br.gov.frameworkdemoiselle.transaction.Transactional;
import javax.inject.Inject;

/**
 *
 * @author Ezequiel Juliano Müller
 */
@BusinessController
public class UsuarioBC extends DelegateCrud<Usuario, String, UsuarioDAO> {
    
    private static final long serialVersionUID = 1L;
    @Inject
    private UsuarioDAO dao;
    @Inject
    private EntidadeBC entidadeBC;
    
    public void saveOrUpdate(Usuario usuario) throws ValidationException {
        //Verifica se nome de usuário já existe
        Usuario objUserName = findByUserName(usuario.getUserName());
        if (objUserName != null && !objUserName.getId().equals(usuario.getId())) {
            throw new ValidationException("Nome de usuário já cadastrado!");
        }
        //Verifica se o e-mail já existe
        Usuario objEmail = findByEmail(usuario.getEmail());
        if (objEmail != null && !objEmail.getId().equals(usuario.getId())) {
            throw new ValidationException("E-mail já cadastrado!");
        }
        //Verifica se é usuário normal
        //Caso for deve ser informada a entidade
        if ((usuario.getPerfil().equals(UsuarioPerfil.normal)) && (usuario.getEntidade() == null)) {
            throw new ValidationException("Usuário Normal deve possuir uma entidade!");
        }
        //Gerar Api Key
        if (usuario.getApiKey() == null) {
            generateApiKey(usuario);
        }
        //Procedimento de Salvar 
        if (usuario.getId() == null) {
            usuario.setId(Utils.getUniqueId());
            //Gerar Hash da senha
            generatePasswordKey(usuario);
            insert(usuario);
        } else {
            update(usuario);
        }
    }
    
    public void inativar(Usuario usuario) throws ValidationException {
        if (usuario.isAtivo()) {
            usuario.setSituacao(Situacao.inativo);
            saveOrUpdate(usuario);
        }
    }
    
    public void ativar(Usuario usuario) throws ValidationException {
        if (usuario.isInativo()) {
            usuario.setSituacao(Situacao.ativo);
            saveOrUpdate(usuario);
        }
    }
    
    public Usuario findByUserName(String userName) {
        return dao.findByUserName(userName);
    }
    
    public Usuario findByEmail(String email) {
        return dao.findByEmail(email);
    }
    
    public void generateApiKey(Usuario usuario) {
        String apiKey = UniqId.getInstance().getUniqIDHashString();
        usuario.setApiKey(apiKey);
    }
    
    public void generatePasswordKey(Usuario usuario) {
        String passwordKey = UniqId.getInstance().hashString(usuario.getPassword());
        usuario.setPassword(passwordKey);
    }
    
    public Usuario login(String userName, String password) {
        String passwordKey = UniqId.getInstance().hashString(password);
        return dao.login(userName, passwordKey);
    }
    
    @Startup
    @Transactional
    public void insereUsuarioPadrao() throws ValidationException {
        if (findByUserName("adm") == null) {
            Usuario user = new Usuario();
            user.setUserName("adm");
            user.setEmail("adm@adm.com.br");
            user.setPassword("123");
            user.setPerfil(UsuarioPerfil.administrador);
            user.setSituacao(Situacao.ativo);
            saveOrUpdate(user);
        }
        
//        if (findByUserName("normal") == null) {
//            Usuario user = new Usuario();
//            user.setUserName("normal");
//            user.setEmail("normal@normal.com.br");
//            user.setPassword("123");
//            user.setEntidade(entidadeBC.findByCodigo(1));
//            user.setPerfil(UsuarioPerfil.normal);
//            user.setSituacao(Situacao.ativo);
//            saveOrUpdate(user);
//        }
        
    }
}
