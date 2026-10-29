package social.ahtara.authentication.domain

import android.content.Context
import android.content.Intent
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import social.ahtara.authentication.R









enum class AccountType(
    val activityClassName: String,
    @StringRes val title: Int,
    @StringRes val description: Int,
    @StringRes val signUpTitle: Int,
    @StringRes val setupTitle: Int,
    @StringRes val nameLabel: Int,
    @DrawableRes val icon: Int,
    @ColorRes val accentColor: Int,
    
    @DrawableRes val illustration: Int,
    
    @StringRes val industryLabel: Int? = null,
    @StringRes val registrationLabel: Int? = null,
) {
    PERSONAL(
        activityClassName = "social.ahtara.personal.PersonalRegistrationActivity",
        title = R.string.authentication_account_personal,
        description = R.string.authentication_account_personal_desc,
        signUpTitle = R.string.authentication_title_sign_up_personal,
        setupTitle = R.string.authentication_title_setup_personal,
        nameLabel = R.string.authentication_label_name_personal,
        icon = R.drawable.authentication_ic_personal,
        accentColor = R.color.authentication_accent_personal,
        illustration = R.drawable.authentication_illus_personal,
    ),
    BUSINESS(
        activityClassName = "social.ahtara.business.BusinessRegistrationActivity",
        title = R.string.authentication_account_business,
        description = R.string.authentication_account_business_desc,
        signUpTitle = R.string.authentication_title_sign_up_business,
        setupTitle = R.string.authentication_title_setup_business,
        nameLabel = R.string.authentication_label_name_business,
        icon = R.drawable.authentication_ic_business,
        accentColor = R.color.authentication_accent_business,
        illustration = R.drawable.authentication_illus_business,
        industryLabel = R.string.authentication_label_industry_business,
        registrationLabel = R.string.authentication_label_registration_business,
    ),
    ORGANIZATION(
        activityClassName = "social.ahtara.organization.OrganizationRegistrationActivity",
        title = R.string.authentication_account_organization,
        description = R.string.authentication_account_organization_desc,
        signUpTitle = R.string.authentication_title_sign_up_organization,
        setupTitle = R.string.authentication_title_setup_organization,
        nameLabel = R.string.authentication_label_name_organization,
        icon = R.drawable.authentication_ic_organization,
        accentColor = R.color.authentication_accent_organization,
        illustration = R.drawable.authentication_illus_organization,
        industryLabel = R.string.authentication_label_industry_organization,
        registrationLabel = R.string.authentication_label_registration_organization,
    );

    
    val hasDetails: Boolean get() = industryLabel != null

    
    fun intent(context: Context): Intent = Intent().setClassName(context, activityClassName)

    companion object {
        
        fun fromName(name: String?): AccountType = entries.firstOrNull { it.name == name } ?: PERSONAL

        /**
         * Account types offered to users right now. Only Personal is implemented; BUSINESS and
         * ORGANIZATION are stubs. Add them here once their registration modules and databases exist
         * (with more than one offered, the pre-Home "Account type" step is shown).
         */
        val available: List<AccountType> = listOf(PERSONAL)
    }
}