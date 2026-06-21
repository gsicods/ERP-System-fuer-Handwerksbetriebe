package org.example.kalkulationsprogramm.config

import org.example.kalkulationsprogramm.domain.FrontendUserRole
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class FrontendUserPrincipal(
    val id: Long?,
    private val usernameValue: String?,
    val displayName: String?,
    private val passwordHash: String?,
    private val active: Boolean,
    roles: Set<FrontendUserRole>?
) : UserDetails {
    val roles: Set<FrontendUserRole> = roles?.toSet() ?: emptySet()
    private val mappedAuthorities: Set<GrantedAuthority> = this.roles
        .mapTo(LinkedHashSet()) { SimpleGrantedAuthority("ROLE_${it.name}") }

    fun hasRole(role: FrontendUserRole?): Boolean = role != null && roles.contains(role)

    override fun getAuthorities(): Collection<GrantedAuthority> = mappedAuthorities
    override fun getPassword(): String? = passwordHash
    override fun getUsername(): String? = usernameValue
    override fun isAccountNonExpired(): Boolean = true
    override fun isAccountNonLocked(): Boolean = true
    override fun isCredentialsNonExpired(): Boolean = true
    override fun isEnabled(): Boolean = active
}
