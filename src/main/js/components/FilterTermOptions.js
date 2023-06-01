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

class FilterTermOptions extends React.Component {

    constructor(props) {
        super(props);

        this.handleTermClick.bind(this)
        this.getWorkingTermList.bind(this)
    }
    
    handleShowTermsClick = (event) => {
        if (this.props.showOnlyActiveTerms) {
            this.props.updateStateBatch({showOnlyActiveTerms: false, moreTermsClick: true});
        } else {
            this.props.updateStateBatch({showOnlyActiveTerms: true, fewerTermsClick: true});
        }
            
        // Since this is a link, the default behavior when a link is clicked is to
        // scroll to the top of the screen. We don't want that
        event.preventDefault();
    }

    handleTermClick = (event) => {
        var value = event.target.value;
        var checked = event.target.checked;

        const data = this.props.handleFilterBatch(value, checked)
        this.props.updateStateBatch(data);

    }

    getWorkingTermList = (showOnlyActiveTerms) => {
        return showOnlyActiveTerms ? this.props.activeTerms : this.props.allTerms;
    }

    render() {
        var showOnlyActiveTerms = this.props.showOnlyActiveTerms;

        //Which list of terms should we use?
        var termList = this.getWorkingTermList(showOnlyActiveTerms);

        const terms = termList.map((term) => (
            <li key={term.id}>
                <div class="rvt-checkbox">
                    <input type="checkbox" id={`term_${term.id}`} name="termCheckboxes" className="filter-input" value={term.id}
                        onChange={this.handleTermClick} checked={this.props.filteredTerms.includes(term.id)} />
                    <label htmlFor={`term_${term.id}`} className="rvt-m-right-sm rvt-text-nobr">{term.name}</label>
                </div>
            </li>
        ))

        let showTermsLink;
        if (this.props.allTerms.length != this.props.activeTerms.length) {
            var linkText = showOnlyActiveTerms ? 'Show More' : 'Show Less';
            showTermsLink = (
                        <a id="showTerms" className="rvt-link-bold showMoreTerms iconPointer"
                            onClick={this.handleShowTermsClick} href="#">{linkText}</a>
                    )
        }

        return (
            <React.Fragment>
                <ul className="rvt-list-plain">
                    {terms}
                </ul>
                {showTermsLink}
            </React.Fragment>
        );
    }
}

export default FilterTermOptions
