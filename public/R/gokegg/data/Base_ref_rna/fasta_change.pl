#!/usr/bin/perl -w
use strict;
use warnings;
use Bio::SeqIO;
my $input = shift;
my $output = shift;
my $in  = Bio::SeqIO->new(-file => "$input",
                       -format => 'Fasta');
my $out = Bio::SeqIO->new(-file => ">$output",
                       -format => 'Fasta');
while ( my $seq = $in->next_seq() ) {$out->write_seq($seq); }
