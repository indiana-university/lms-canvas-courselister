import React from 'react'

class FilterTermOptions extends React.Component {

    constructor(props) {
        super(props);

        this.handleShowMoreTermsClick.bind(this)
        this.handleShowFewerTermsClick.bind(this)
        this.handleTermClick.bind(this)
        this.getWorkingTermList.bind(this)
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

    getWorkingTermList = (showOnlyActiveTerms) => {
        return showOnlyActiveTerms ? this.props.activeTerms : this.props.allTerms;
    }

    render() {
        var showOnlyActiveTerms = this.props.showOnlyActiveTerms;

        //Which list of terms should we use?
        var termList = this.getWorkingTermList(showOnlyActiveTerms);

        const terms = termList.map((term) => (
            <li key={term.id}>
                <input type="checkbox" id={`term_${term.id}`} name="termCheckboxes" className="filter-input" value={term.id}
                    onChange={this.handleTermClick} checked={this.props.filteredTerms.includes(term.id)} />
                <label htmlFor={`term_${term.id}`} className="rvt-m-right-sm rvt-text-nobr">{term.name}</label>
            </li>
        ))

        //Only show the "More" link if there are more terms to show
        let showMoreTermsLink;
        if (showOnlyActiveTerms && this.props.allTerms.length != this.props.activeTerms.length) {
            showMoreTermsLink = (
                <a id="showMoreTerms" className="rvt-link-bold showMoreTerms iconPointer"
                    onClick={this.handleShowMoreTermsClick} href="#">Show More</a>
            )
        }

        //Only show the "Fewer" link if there were previously more terms to show
        let showFewerTermsLink;
        if (!showOnlyActiveTerms && this.props.allTerms.length != this.props.activeTerms.length) {
            showFewerTermsLink = (
                <a id="showFewerTerms" className="rvt-link-bold showMoreTerms iconPointer"
                    onClick={this.handleShowFewerTermsClick} href="#">Show Less</a>
            )
        }

        return (
            <React.Fragment>
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
