/*
 * TeleStax, Open Source Cloud Communications
 * Copyright 2011-2014, Telestax Inc and individual contributors
 * by the @authors tag.
 *
 * This program is free software: you can redistribute it and/or modify
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 3 of
 * the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.restcomm.connect.dao.common;

import java.util.List;

import javax.servlet.sip.SipServletRequest;
import javax.servlet.sip.SipURI;

import org.apache.log4j.Logger;
import org.restcomm.connect.commons.dao.Sid;
import org.restcomm.connect.dao.DaoManager;
import org.restcomm.connect.dao.IncomingPhoneNumbersDao;
import org.restcomm.connect.dao.entities.IncomingPhoneNumber;
import org.restcomm.connect.dao.entities.MostOptimalNumberResponse;
import org.restcomm.connect.dao.entities.Organization;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;

/**
 * @author maria.farooq@telestax.com (Maria Farooq)
 */
public class OrganizationUtil {

    private static Logger logger = Logger.getLogger(OrganizationUtil.class);

    public static MostOptimalNumberResponse getMostOptimalIncomingPhoneNumber(DaoManager storage, SipServletRequest request, String phone,
            Sid fromClientAccountSid, boolean failCall) {
        //TODO remove it before merge
        logger.info("*********************** getMostOptimalIncomingPhoneNumber started ***********************: "+phone);

        IncomingPhoneNumber number = null;
        Sid destinationOrganizationSid = getOrganizationSidBySipURIHost(storage, (SipURI)request.getRequestURI());

        if(destinationOrganizationSid == null){
            logger.error("destinationOrganizationSid is NULL: reuest Uri is: "+(SipURI)request.getRequestURI());
        }else{
            Sid sourceOrganizationSid = null;
            if(fromClientAccountSid != null){
                sourceOrganizationSid = storage.getAccountsDao().getAccount(fromClientAccountSid).getOrganizationSid();
            }

            logger.info("getMostOptimalIncomingPhoneNumber: sourceOrganizationSid: "+sourceOrganizationSid+" : destinationOrganizationSid: "+destinationOrganizationSid);

            // Format the destination to an E.164 phone number.
            final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();
            String formatedPhone = null;
            if (!(phone.contains("*") || phone.contains("#"))) {
                try {
                    formatedPhone = phoneNumberUtil.format(phoneNumberUtil.parse(phone, "US"), PhoneNumberFormat.E164);
                } catch (NumberParseException e) {
                    logger.error("Exception when try to format : " + e);
                }
            }
            if (formatedPhone == null) {
                //Don't format to E.164 if phone contains # or * as this is
                //for a Regex or USSD short number
                formatedPhone = phone;
            }
            List<IncomingPhoneNumber> numbers = null;
            // Try to find an application defined for the phone number.
            final IncomingPhoneNumbersDao numbersDao = storage.getIncomingPhoneNumbersDao();
            //get all number with same number, by both formatedPhone and unformatedPhone
            numbers = numbersDao.getIncomingPhoneNumber(formatedPhone);
            //TODO remove it before merge
            logger.info("getMostOptimalIncomingPhoneNumber: get formatedPhone result size: "+numbers.size());
            numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
            //TODO remove it before merge
            logger.info("getMostOptimalIncomingPhoneNumber: get unformatedPhone result size: "+numbers.size());
            if (phone.startsWith("+")) {
                //remove the (+) and check if exists
                phone= phone.replaceFirst("\\+","");
                numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
                //TODO remove it before merge
                logger.info("getMostOptimalIncomingPhoneNumber: get phone without plus result size: "+numbers.size());
            } else {
                //Add "+" add check if number exists
                phone = "+".concat(phone);
                numbers.addAll(numbersDao.getIncomingPhoneNumber(phone));
                //TODO remove it before merge
                logger.info("getMostOptimalIncomingPhoneNumber: get phone with plus result size: "+numbers.size());
            }
            if(numbers.isEmpty()){
                // https://github.com/Mobicents/RestComm/issues/84 using wildcard as default application
                numbers.addAll(numbersDao.getIncomingPhoneNumber("*"));
            }
            //TODO remove it before merge
            logger.info("getMostOptimalIncomingPhoneNumber: list size after getDistinctNumbersList: "+numbers.size());
            if(!numbers.isEmpty()){
                // find number in same organization
                for(IncomingPhoneNumber n : numbers){
                    //TODO remove it before merge
                    logger.info("getMostOptimalIncomingPhoneNumber: sourceOrganizationSid: "+sourceOrganizationSid+" destinationOrganizationSid: "+destinationOrganizationSid+" n.isPureSip(): "+n.isPureSip());
                    if(n.getOrganizationSid().equals(destinationOrganizationSid)){
                        /*
                         * check if request is coming from same org
                         * if not then only allow provider numbers
                         */
                        if((sourceOrganizationSid != null && sourceOrganizationSid.equals(destinationOrganizationSid)) || (sourceOrganizationSid == null) || !n.isPureSip()){
                            number = n;
                            //TODO remove it before merge
                            logger.info("found number: "+number+" | org: "+n.getOrganizationSid()+" | isPureSip: "+n.isPureSip());
                        }
                    }
                    if(number != null)
                        break;
                }
                failCall = number == null;
            }
        }
        //TODO remove it before merge
        logger.info("*********************** getMostOptimalIncomingPhoneNumber ended ***********************"+number);
        return new MostOptimalNumberResponse(number, failCall);
    }


    /**
     * getOrganizationSidBySipURIHost
     *
     * @param sipURI
     * @return Sid of Organization
     */
    public static Sid getOrganizationSidBySipURIHost(DaoManager storage, final SipURI sipURI){
        final String organizationDomainName = sipURI.getHost();
        Organization organization = storage.getOrganizationsDao().getOrganizationByDomainName(organizationDomainName);
        return organization == null ? null : organization.getSid();
    }
}