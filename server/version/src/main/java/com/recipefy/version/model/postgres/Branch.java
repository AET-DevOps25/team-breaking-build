package com.recipefy.version.model.postgres;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = true)
@Entity
public class Branch extends BaseEntity {

    private String name;

    private Long recipeId;

    @ManyToOne
    @JoinColumn(name = "head_commit_id")
    private Commit headCommit;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "branch_commits",
            joinColumns = @JoinColumn(name = "branch_id"),
            inverseJoinColumns = @JoinColumn(name = "commit_id")
    )
    private Set<Commit> commits = new HashSet<>();

    public void addCommit(Commit commit) {
        headCommit = commit;
        commits.add(commit);
    }

    public static Branch copyFrom(Branch sourceBranch, String newName, Long recipeId) {
        Branch newBranch = new Branch();
        newBranch.setName(newName);
        newBranch.setRecipeId(recipeId);
        newBranch.setHeadCommit(sourceBranch.getHeadCommit());
        newBranch.commits.addAll(sourceBranch.getCommits());
        return newBranch;
    }
}
