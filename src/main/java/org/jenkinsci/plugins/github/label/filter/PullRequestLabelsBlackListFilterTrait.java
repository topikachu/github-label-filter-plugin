/*
 * The MIT License
 *
 * Copyright (c) 2017, Shantur Rathore.
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
package org.jenkinsci.plugins.github.label.filter;

import edu.umd.cs.findbugs.annotations.NonNull;
import hudson.Extension;
import hudson.console.HyperlinkNote;
import jenkins.scm.api.SCMHead;
import jenkins.scm.api.trait.SCMHeadFilter;
import jenkins.scm.api.trait.SCMSourceRequest;
import jenkins.scm.impl.trait.Discovery;
import org.jenkinsci.plugins.github_branch_source.GitHubSCMSourceRequest;
import org.jenkinsci.plugins.github_branch_source.PullRequestSCMHead;
import org.kohsuke.github.GHLabel;
import org.kohsuke.stapler.DataBoundConstructor;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * A {@link Discovery} trait for GitHub that will only select pull requests that have specified label.
 */
public class PullRequestLabelsBlackListFilterTrait extends BaseGithubExtendedFilterTrait {

	private SCMHeadFilter scmHeadFilter;

	/**
	 * Constructor for stapler.
	 *
	 * @param labels Labels for filtering pull request labels
	 */
	@DataBoundConstructor
	public PullRequestLabelsBlackListFilterTrait(String labels) {
		super(labels);
	}

	protected SCMHeadFilter getScmHeadFilter() {
		SCMHeadFilter scmHeadFilter = new SCMHeadFilter() {

			@Override
			public boolean isExcluded(@NonNull SCMSourceRequest request, @NonNull SCMHead head) {
				if (request instanceof GitHubSCMSourceRequest && head instanceof PullRequestSCMHead) {
					List<String> foundLabels = getPullRequestLabels((GitHubSCMSourceRequest)request, (PullRequestSCMHead) head);
					List<String> blacklistLabels = getLabelsAsList();
					if (blacklistLabels.isEmpty()) {
						request.listener().getLogger().format("%n  No labels are defined in the trait. Includes this pull request.%n");
						return false;
					}
					if (foundLabels.isEmpty()) {
						request.listener().getLogger().format("%n  Has no labels. Includes this pull request.%n");
						return false;
					}
					boolean isExcluded = foundLabels.stream()
							.filter(blacklistLabels::contains)
							.count()>0;
					if (isExcluded ) {
						request.listener().getLogger().format("%n  Contains the blacklist labels \"%s\". Skipped.%n", String.join(",", blacklistLabels));
					} else {
						request.listener().getLogger().format("%n  Doesn't contain the blacklist labels \"%s\". Includes this pull request.%n", String.join(",", blacklistLabels));
					}
					return isExcluded;

				}
				return false;
			}
		};
		return scmHeadFilter;
	}


	@Extension
	@Discovery
	public static class DescriptorImpl extends BaseDescriptorImpl {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public String getDisplayName() {
			return "Blacklist pull requests matching any labels";
		}

	}

}