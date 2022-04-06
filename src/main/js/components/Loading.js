/*-
 * #%L
 * lms-lti-courselist
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */
import React from 'react'

const Loading = (props) => {
    if (props.loading) {
        const svg = {"width" : 50, "height" : 50, "color" : "#006298"};
        return (
            <div className="rvt-m-tb-xl rvt-container rvt-container--center">
                <div className="rvt-grid loader_container">
                    <div className="rvt-grid__item rvt-text-right">
                        <svg width={svg.width} height={svg.height} version="1.1" id="L2" xmlns="http://www.w3.org/2000/svg" x="0px" y="0px" viewBox="0 0 100 100" enableBackground="new 0 0 100 100" xmlSpace="preserve">
                            <circle fill="none" stroke={svg.color} strokeWidth="4" strokeMiterlimit="10" cx="50" cy="50" r="48"></circle>
                            <line fill="none" strokeLinecap="round" stroke={svg.color} strokeWidth="4" strokeMiterlimit="10" x1="50" y1="50" x2="85" y2="50.5" transform="rotate(76.5634 50 50)">
                                <animateTransform attributeName="transform" dur="2s" type="rotate" from="0 50 50" to="360 50 50" repeatCount="indefinite"></animateTransform>
                            </line>
                            <line fill="none" strokeLinecap="round" stroke={svg.color} strokeWidth="4" strokeMiterlimit="10" x1="50" y1="50" x2="49.5" y2="74" transform="rotate(274.208 50 50)">
                                <animateTransform attributeName="transform" dur="15s" type="rotate" from="0 50 50" to="360 50 50" repeatCount="indefinite"></animateTransform>
                            </line>
                        </svg>
                    </div>
                    <div className="rvt-grid__item rvt-text-left">
                        <span className="rvt-ts-md ">Loading...</span>
                    </div>
                </div>
            </div>
        )
    } else {
        return null;
    }
}

export default Loading
