#!usr/bin/perl -w

use strict;

die "usage:perl $0 <gene_ontology.1_2.obo> \n" unless @ARGV == 1;

open A,$ARGV[0];
my %tr=(
    "biological_process"=>'BP',
    "cellular_component"=>'CC',
    "molecular_function"=>'MF'
);

my %h1;my %tar;my %rela;my %h2;
$/="Term]\nid: ";
<A>;
while(<A>){
    chomp;my @a=split /\n+/;
    my @b=split /: /,$a[1]; #function
    my @c=split /: /,$a[2]; #main function
    #print "$a[0]\t$b[1]\t$c[1]\n";
    my @alt;
    foreach my $k1(3..$#a){
        my @dd=split /\s+/,$a[$k1];
        my @func=split /\! /,$a[$k1];
        if($a[$k1]=~/alt_id:/){
            push @alt,$dd[1];
        }
        if($a[$k1]=~/is_a:/){
            print "$a[0]\t$dd[1]\t$tr{$c[1]}\t$b[1]\n";
            foreach my $k2(@alt){
                print "$k2\t$dd[1]\t$tr{$c[1]}\t$b[1]\n";
            }
        }
        if($a[$k1]=~/consider:/){
            print "$a[0]\t$dd[1]\t$tr{$c[1]}\t$b[1]\n"; 
            foreach my $k2(@alt){
                print "$k2\t$dd[1]\t$tr{$c[1]}\t$b[1]\n";
            }
        }
        if($a[$k1]=~/relationship:/){
            print "$a[0]\t$dd[2]\t$tr{$c[1]}\t$b[1]\n";
            foreach my $k2(@alt){
                print "$k2\t$dd[2]\t$tr{$c[1]}\t$b[1]\n";
            }
        }
    }
}
close A;
$/="\n";
