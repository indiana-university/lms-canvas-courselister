import React from 'react'

class FilterTermOptions extends React.Component {

    constructor(props) {
        super(props);

        this.handleShowMoreTermsClick.bind(this)
        this.handleShowFewerTermsClick.bind(this)
        this.handleTermClick.bind(this)
        this.handleCheckUncheckAll.bind(this)
        this.getWorkingTermList.bind(this)
        this.determineCheckAllText.bind(this)
    }

    determineCheckAllText = (showOnlyActiveTerms) => {
        var termList = this.getWorkingTermList(showOnlyActiveTerms);
        var filteredTerms = this.props.filteredTerms;

        var text = 'Select All';
        if (termList.length <= filteredTerms.length) {
            text = 'Unselect All'
        }
        return text;
    }

    handleShowMoreTermsClick = (event) => {
        this.props.updateStateBatch({showOnlyActiveTerms: false, moreTermsClick: true});
    }

    handleShowFewerTermsClick = (event) => {
        this.props.updateStateBatch({showOnlyActiveTerms: true, fewerTermsClick: true});
    }

    handleTermClick = (event) => {
        var value = event.target.value;
        var checked = event.target.checked;

        const data = this.props.handleFilterBatch(value, checked)
        this.props.updateStateBatch(data);

    }

    handleCheckUncheckAll = (event) => {
        var allTerms = this.props.allTerms;

        let batchedProps;

        var newChecked = 'Select All' === this.determineCheckAllText(this.props.showOnlyActiveTerms);

        for (var i=0; i<allTerms.length; ++i) {
            var termId = allTerms[i].id;
            var cb = document.querySelector("input#term_" + termId);

            //Do the visible checkbox
            if (cb && cb.checked != newChecked) {
                //But only if the value is going to change
                cb.checked = newChecked;
                batchedProps = this.props.handleFilterBatch(cb.value, newChecked, batchedProps)
            } else if (!cb) {
                //Do any "hidden" terms too
                batchedProps = this.props.handleFilterBatch(termId, newChecked, batchedProps)
            }
        }
        this.props.updateStateBatch(batchedProps);
    }

    getWorkingTermList = (showOnlyActiveTerms) => {
        return showOnlyActiveTerms ? this.props.activeTerms : this.props.allTerms;
    }

    render() {
        var showOnlyActiveTerms = this.props.showOnlyActiveTerms;
        var checkAllText = this.determineCheckAllText(showOnlyActiveTerms);

        //Which list of terms should we use?
        var termList = this.getWorkingTermList(showOnlyActiveTerms);

        const terms = termList.map((term) => (
            <li key={term.id}>
                <input type="checkbox" id={`term_${term.id}`} name="termCheckboxes" className="filter-input" value={term.id}
                    onChange={this.handleTermClick} checked={this.props.filteredTerms.includes(term.id)} />
                <label htmlFor={`term_${term.id}`} className="rvt-m-right-sm rvt-text-nobr">{term.name}</label>
            </li>
        ))

        //Only show the "All Terms" option if there is more than one term
        let allTermsOption;
        if (termList.length > 1) {
            allTermsOption = (
                <a id="checkAllTerms" className="rvt-link-bold showMoreTerms iconPointer"
                    href="#" onClick={this.handleCheckUncheckAll}>{checkAllText}</a>
            )
        }

        //Only show the "More" link if there are more terms to show
        let showMoreTermsLink;
        if (showOnlyActiveTerms && this.props.allTerms.length != this.props.activeTerms.length) {
            showMoreTermsLink = (
                <a id="showMoreTerms" className="rvt-link-bold showMoreTerms iconPointer"
                    onClick={this.handleShowMoreTermsClick}>Show More</a>
            )
        }

        //Only show the "Fewer" link if there were previously more terms to show
        let showFewerTermsLink;
        if (!showOnlyActiveTerms && this.props.allTerms.length != this.props.activeTerms.length) {
            showFewerTermsLink = (
                <a id="showFewerTerms" className="rvt-link-bold showMoreTerms iconPointer"
                    onClick={this.handleShowFewerTermsClick}>Show Less</a>
            )
        }

        return (
            <React.Fragment>
                {allTermsOption}
                <ul className="rvt-plain-list">
                    {terms}
                </ul>
                {showMoreTermsLink}
                {showFewerTermsLink}
            </React.Fragment>
        );
    }
}

export default FilterTermOptions
