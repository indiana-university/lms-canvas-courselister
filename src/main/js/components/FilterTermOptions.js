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
                <input type="checkbox" id={`term_${term.id}`} name="termCheckboxes" className="filter-input" value={term.id}
                    onChange={this.handleTermClick} checked={this.props.filteredTerms.includes(term.id)} />
                <label htmlFor={`term_${term.id}`} className="rvt-m-right-sm rvt-text-nobr">{term.name}</label>
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
                <ul className="rvt-plain-list">
                    {terms}
                </ul>
                {showTermsLink}
            </React.Fragment>
        );
    }
}

export default FilterTermOptions
