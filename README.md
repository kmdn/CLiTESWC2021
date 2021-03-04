# Combining Linking Techniques (ESWC2021 Demo Submission)
/backend contains CLiT's backend logic
/frontend contains CLiT's User interface

Classical Pipeline {#sec:ClassicalPipeline}
------------------

While [el]{acronym-label="el" acronym-form="singular+short"} systems
vary in terms of approaches and potential steps within respective
pipelines, we identify the most commonly-employed ones as the *classical
pipeline*. We use said pipeline as a template for our framework in order
to reach compatibility with as many existing systems as possible. In
Figure [\[fig:classicalpipeline\]](#fig:classicalpipeline){reference-type="ref"
reference="fig:classicalpipeline"}, we present our understanding of the
functioning of a classical pipeline for a single system.

### Input Document

Representing the starting point of any annotation framework, an input
document consists of plain text of some form, generally not bringing any
additional information for systems to take into account. The document
may be part of an evaluation data set or simple text - in both cases
only text is given as information to the successive step.

### Mention Detection

\
\

  ------------- ------------------------------------------------------------------
     **Input:** Text Document
    **Output:** Collection of mentions and their locations within given document
  ------------- ------------------------------------------------------------------

\
\
Also referred to as *spotting*, this task refers to the detection of
so-called *mentions* within a given plain text. Depending on system,
different kinds of mentions may be detected. [pos]{acronym-label="pos"
acronym-form="singular+short"} tagging, [ner]{acronym-label="ner"
acronym-form="singular+short"}-based techniques and string-matching to
labels of specific types of entities from considered knowledge bases are
among potential techniques. Further information pertaining to the
mention is sometimes passed on to a system's consequent step. From the
given text, mentions are extracted and passed on to the following step.

### Candidate Generation

\
\

  ------------- ---------------------------------------------------
     **Input:** Collection of mentions
    **Output:** Collection of candidate entities for each mention
  ------------- ---------------------------------------------------

\
\
Receiving a list of mentions, the process of *candidate generation*
finds lists of appropriate entities for each passed mention. Some
approaches additionally rank candidates at this step e.g. in terms of
general importance based on statistical measures.

### Entity Disambiguation

\
\

  ------------- ---------------------------------------------------
     **Input:** Collection of candidate entities
    **Output:** Disambiguated entities, either collections ranked
                by likelihood or at most one per mention
  ------------- ---------------------------------------------------

\
\
Part of the pipeline potentially granting the most sophisticated
techniques. Generally, this step is based on statistical measures,
allowing for the synthetization of contexts based on detected mentions
and suggested candidates.

### Results

Finally, results from the given pipeline are returned. These tend to be
in the form of annotations based on the initial input document or as
hyperlinks referring to specific knowledge bases, such as
Wikipedia [@wikipedia], DBpedia [@dbpedia] or Wikidata [@wikidata].
Occasionally, some systems add an additional step for *pruning*

Pipeline Customization {#sec:pipelinecustomization}
----------------------

In order to allow for customized experiences and settings, we introduce
- additionally to the singular steps described in
Section [5.1](#sec:ClassicalPipeline){reference-type="ref"
reference="sec:ClassicalPipeline"} - further processing possibilities
with the intent of allowing for nigh-infinite combinations of system
components. The following **subcomponents** place themselves
ideologically *in between* components presented within the classical
model of an [el]{acronym-label="el" acronym-form="singular+short"}
pipeline. We refer to them as *processors* or *subcomponents*, handling
post-processing of structures output from prior tasks, preparing them
for being potentially, in turn, further processed by subsequent steps in
the chosen workflow. In this paper, we define 4 types of processors:
*splitter*s *combiner*s, *filter*s and *translator*s.

### Splitter

\
\

  ------------------- ---------------------------------------------------------------
     **Preceded by:** Any single-connected component
    **Succeeded by:** 2 or more components
        **Commonly:** Directly passing same information to two (or more) components
  ------------------- ---------------------------------------------------------------

\
\
Allowing for processing of items prior to passing them on to a
subsequent step, a splitter is utilised in the case of a single stream
of data being sent to multiple components, potentially warranting
specific splitting of data streams. This step encompasses both a
post-processing step for a prior component, as well as a pre-processing
step for a following one. A potential post-processing step may be to
filter information from a prior step, such as eliminating superfluous
candidate entities or unwanted mentions.

As for pre-processing, a splitter may preprocessing --\> translate from
one KB to another allows for processing of entities resulting from a
prior st

### Combiner

\
\

  ------------------- -----------------------------------------------------------
     **Preceded by:** Any multiply-connected ($\ge$2) component or subcomponent
    **Succeeded by:** Any single component, *translator* or *filter*
        **Commonly:** *Union* operation, *intersection* operation
  ------------------- -----------------------------------------------------------

\
\
In case multiple components were utilised in a prior step and are meant
to be consolidated through a variety of possible combinations actions, a
*combiner* subcomponent must be utilised. It combines results from
multiple inputs into a single output, passing merged partial results on
to a subsequent component. Common operations include *union* - taking
information from multiple sources and adding it together - and
*intersection* - checking multiple sources for certainty of information
prior to passing it on.

### Filter

\
\

  ------------------- -------------------------------------------------------------------------------------------------------------------------------------------------------
     **Preceded by:** Any component or subcomponent.
    **Succeeded by:** Any component or *translator*.
        **Commonly:** [ner]{acronym-label="ner" acronym-form="singular+short"}-, [pos]{acronym-label="pos" acronym-form="singular+short"}-specific or `rdf:type` filtering.
  ------------------- -------------------------------------------------------------------------------------------------------------------------------------------------------

\
\
In order to allow removal of particular sets of items through
user-defined rules or dynamic filtering, we introduce a subcomponent
capable of processing results on binary classifiers: a *filter*. The
truth values evaluated on passed partial results define which further
outcomes may be detected by a subsequent component or translator.

### Translator

\
\

  ------------------- --------------------------------------------------------------------------------------
     **Preceded by:** Any component or subcomponent.
    **Succeeded by:** Any component or subcomponent.
        **Commonly:** `owl:sameAs` linking across [kg]{acronym-label="kg" acronym-form="singular+short"}s.
  ------------------- --------------------------------------------------------------------------------------

\
\
Enabling seamless use of annotation tools regardless of underlying
[kg]{acronym-label="kg" acronym-form="singular+short"}, we introduce the
translator subcomponent. It is meant as a processing unit capable of
translating entities and potentially other features used by one tool to
another, allowing further inter-system compatibility. It may be employed
at any level and succeeded by any (sub)component due to its ubiquitous
characteristics and necessity when working with heterogeneous systems.

Protocol Development
--------------------