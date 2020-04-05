/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.niis.xroad.restapi.service;

import lombok.extern.slf4j.Slf4j;
import org.niis.xroad.restapi.domain.InvalidRoleNameException;
import org.niis.xroad.restapi.domain.PersistentApiKeyType;
import org.niis.xroad.restapi.domain.Role;
import org.niis.xroad.restapi.exceptions.ErrorDeviation;
import org.niis.xroad.restapi.repository.ApiKeyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * ApiKey service.
 * Uses simple caching, using ConcurrentHashMaps in memory.
 */
@Slf4j
@Service
@Transactional
public class ApiKeyService {

    // two caches
    public static final String GET_KEY_CACHE = "apikey-by-keys";
    public static final String LIST_ALL_KEYS_CACHE = "all-apikeys";

    private final PasswordEncoder passwordEncoder;
    private final ApiKeyRepository apiKeyRepository;

    @Autowired
    public ApiKeyService(PasswordEncoder passwordEncoder, ApiKeyRepository apiKeyRepository) {
        this.passwordEncoder = passwordEncoder;
        this.apiKeyRepository = apiKeyRepository;
    }

    /**
     * Api keys are created with UUID.randomUUID which uses SecureRandom,
     * which is cryptographically secure.
     * @return
     */
    @PreAuthorize("isAuthenticated()")
    private String createApiKey() {
        return UUID.randomUUID().toString();
    }

    /**
     * create api key with one role
     */
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(allEntries = true, cacheNames = { LIST_ALL_KEYS_CACHE, GET_KEY_CACHE })
    public PersistentApiKeyType create(String roleName) throws InvalidRoleNameException {
        return create(Collections.singletonList(roleName));
    }

    /**
     * create api key with collection of roles
     * @return new PersistentApiKeyType that contains the new key in plain text
     * @throws InvalidRoleNameException if roleNames was empty or contained invalid roles
     */
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(allEntries = true, cacheNames = { LIST_ALL_KEYS_CACHE, GET_KEY_CACHE })
    public PersistentApiKeyType create(Collection<String> roleNames)
            throws InvalidRoleNameException {
        if (roleNames.isEmpty()) {
            throw new InvalidRoleNameException("missing roles");
        }
        Set<Role> roles = Role.getForNames(roleNames);
        String plainKey = createApiKey();
        String encodedKey = encode(plainKey);
        PersistentApiKeyType apiKey = new PersistentApiKeyType(plainKey, encodedKey,
                Collections.unmodifiableCollection(roles));
        apiKeyRepository.saveOrUpdate(apiKey);
        return apiKey;
    }

    /**
     * update api key with one role by key id
     * @param id
     * @param roleName
     * @throws InvalidRoleNameException if roleNames was empty or contained invalid roles
     * @throws ApiKeyService.ApiKeyNotFoundException if api key was not found
     */
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(allEntries = true, cacheNames = { LIST_ALL_KEYS_CACHE, GET_KEY_CACHE })
    public PersistentApiKeyType update(long id, String roleName)
            throws InvalidRoleNameException, ApiKeyService.ApiKeyNotFoundException {
        return update(id, Collections.singletonList(roleName));
    }

    /**
     * update api key with collection of roles by key id
     * @param id
     * @param roleNames
     * @return
     * @throws InvalidRoleNameException if roleNames was empty or contained invalid roles
     * @throws ApiKeyService.ApiKeyNotFoundException if api key was not found
     */
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(allEntries = true, cacheNames = { LIST_ALL_KEYS_CACHE, GET_KEY_CACHE })
    public PersistentApiKeyType update(long id, Collection<String> roleNames)
            throws InvalidRoleNameException, ApiKeyService.ApiKeyNotFoundException {
        PersistentApiKeyType apiKeyType = apiKeyRepository.getApiKey(id);
        if (apiKeyType == null) {
            throw new ApiKeyService.ApiKeyNotFoundException("api key with id " + id + " not found");
        }
        if (roleNames.isEmpty()) {
            throw new InvalidRoleNameException("missing roles");
        }
        Set<Role> roles = Role.getForNames(roleNames);
        apiKeyType.setRoles(roles);
        apiKeyRepository.saveOrUpdate(apiKeyType);
        return apiKeyType;
    }

    @PreAuthorize("isAuthenticated()")
    private String encode(String key) {
        return passwordEncoder.encode(key);
    }

    /**
     * get matching key
     * @param key
     * @return
     * @throws ApiKeyService.ApiKeyNotFoundException if api key was not found
     */
    @Cacheable(GET_KEY_CACHE)
    public PersistentApiKeyType get(String key) throws ApiKeyService.ApiKeyNotFoundException {
        String encodedKey = passwordEncoder.encode(key);
        List<PersistentApiKeyType> keys = apiKeyRepository.getAllApiKeys();
        for (PersistentApiKeyType apiKeyType : keys) {
            if (apiKeyType.getEncodedKey().equals(encodedKey)) {
                return apiKeyType;
            }
        }
        throw new ApiKeyService.ApiKeyNotFoundException("api key not found");
    }

    /**
     * remove / revoke one key
     * @param key
     * @throws ApiKeyService.ApiKeyNotFoundException if api key was not found
     */
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(allEntries = true, cacheNames = { LIST_ALL_KEYS_CACHE, GET_KEY_CACHE })
    public void remove(String key) throws ApiKeyService.ApiKeyNotFoundException {
        PersistentApiKeyType apiKeyType = get(key);
        apiKeyRepository.delete(apiKeyType);
    }

    /**
     * remove / revoke one key by id
     * @param id
     * @throws ApiKeyService.ApiKeyNotFoundException if api key was not found
     */
    @PreAuthorize("isAuthenticated()")
    @CacheEvict(allEntries = true, cacheNames = { LIST_ALL_KEYS_CACHE, GET_KEY_CACHE })
    public void removeById(long id) throws ApiKeyService.ApiKeyNotFoundException {
        PersistentApiKeyType apiKeyType = apiKeyRepository.getApiKey(id);
        if (apiKeyType == null) {
            throw new ApiKeyService.ApiKeyNotFoundException("api key with id " + id + " not found");
        }
        apiKeyRepository.delete(apiKeyType);
    }

    /**
     * List all keys
     * @return
     */
    @PreAuthorize("isAuthenticated()")
    @Cacheable(LIST_ALL_KEYS_CACHE)
    public List<PersistentApiKeyType> listAll() {
        return apiKeyRepository.getAllApiKeys();
    }

    public static class ApiKeyNotFoundException extends NotFoundException {
        public static final String ERROR_API_KEY_NOT_FOUND = "api_key_not_found";
        ApiKeyNotFoundException(String s) {
            super(s, new ErrorDeviation(ERROR_API_KEY_NOT_FOUND));
        }
    }
}
